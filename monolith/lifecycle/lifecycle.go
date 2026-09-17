package lifecycle

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"payment-system/monolith/store"

	"github.com/google/uuid"
)

// ApiError — callers map Code to envelope.
type ApiError struct {
	Code    int
	Message string
}

func (e *ApiError) Error() string { return e.Message }

// Module prefix LC: answers "which part failed" in one line.
func errBadRequest(msg string) *ApiError   { return &ApiError{Code: 400, Message: "LC: " + msg} }
func errNotFound(msg string) *ApiError     { return &ApiError{Code: 404, Message: "LC: " + msg} }
func errConflict(msg string) *ApiError     { return &ApiError{Code: 409, Message: "LC: " + msg} }
func errUnauthorized(msg string) *ApiError { return &ApiError{Code: 401, Message: "LC: " + msg} }

func NewBadRequest(msg string) *ApiError   { return errBadRequest(msg) }
func NewNotFound(msg string) *ApiError     { return errNotFound(msg) }
func NewConflict(msg string) *ApiError     { return errConflict(msg) }
func NewUnauthorized(msg string) *ApiError { return errUnauthorized(msg) }

// Service is TransactionLifecycle seam: pricing, state chart, idempotency, authz, outbox.
type Service struct {
	store Store
}

type Store = store.Store

func New(s Store) *Service      { return &Service{store: s} }
func (s *Service) Store() Store { return s.store }

const (
	StatusCreated   = "CREATED"
	StatusReady     = "READY"
	StatusSuccess   = "SUCCESS"
	StatusPublished = "PUBLISHED"
	StatusFailed    = "FAILED"
	StatusRefund    = "REFUND"
)

type CreateCmd struct {
	Username       string
	UserID         string
	ProductCode    string
	ProductName    string
	Price          int64
	Amount         int64
	EnableDiscount bool
	Discount       float64
}

type ChargeRs struct {
	ID            string `json:"id"`
	TransactionID string `json:"transactionId"`
	CheckoutURL   string `json:"checkoutUrl"`
	Status        string `json:"status"`
}

type CallbackEvt struct {
	ReferenceID string `json:"referenceId"`
	Status      string `json:"status"` // SUCCEEDED / FAILED
}

type RefundRs struct {
	ID            string `json:"id"`
	TransactionID string `json:"transactionId"`
	Status        string `json:"status"`
}

var transitions = map[string]map[string]string{
	StatusCreated:   {"charge": StatusReady},
	StatusReady:     {"callback_success": StatusSuccess, "callback_failed": StatusFailed, "timeout": StatusFailed},
	StatusSuccess:   {"refund": StatusRefund},
	StatusPublished: {"refund": StatusRefund},
}

func validateTransition(old, event string) (string, error) {
	nxt, ok := transitions[old][event]
	if !ok {
		return "", errConflict(fmt.Sprintf("invalid event %s from %s", event, old))
	}
	return nxt, nil
}

// findTx returns trx or NotFound — replaces 4x if err-or-nil chains.
func (s *Service) findTx(ctx context.Context, txID string) (*store.ProductTrx, error) {
	trx, err := s.store.FindTxForUpdate(ctx, txID)
	if err != nil || trx == nil {
		return nil, errNotFound("transaction not found")
	}
	return trx, nil
}

func cachedTrx(ctx context.Context, st Store, key string) *store.ProductTrx {
	if key == "" {
		return nil
	}
	cached, _ := st.GetIdempotency(ctx, key)
	if cached == "" || cached == "{}" {
		return nil
	}
	var trx store.ProductTrx
	if err := json.Unmarshal([]byte(cached), &trx); err != nil {
		return nil
	}
	return &trx
}

func (s *Service) Check(ctx context.Context, txID, callerUserID string) (*store.ProductTrx, error) {
	trx, err := s.findTx(ctx, txID)
	if err != nil {
		return nil, err
	}
	if trx.UserID != callerUserID {
		return nil, errUnauthorized("unauthorized: transaction does not belong to caller")
	}
	return trx, nil
}

// CreateOrder — idempotent via Idempotency-Key, pricing inside. Stores CREATED + outbox atomically.
func (s *Service) CreateOrder(ctx context.Context, cmd CreateCmd, idemKey string) (*store.ProductTrx, error) {
	if trx := cachedTrx(ctx, s.store, idemKey); trx != nil {
		return trx, nil
	}
	if cmd.Username == "" || cmd.ProductCode == "" || cmd.Amount <= 0 || cmd.Price <= 0 {
		return nil, errBadRequest("invalid order payload")
	}
	priceCharge, err := calcPriceCharge(cmd.Price, cmd.Discount, cmd.Amount, cmd.EnableDiscount)
	if err != nil {
		return nil, errBadRequest(err.Error())
	}
	now := time.Now()
	trx := store.ProductTrx{
		ID: "MSO-" + uuid.NewString(), TransactionID: "PTRX-" + uuid.NewString()[:8],
		OrderStatus: StatusCreated, PaymentStatus: StatusCreated,
		UserID: cmd.UserID, ProductName: cmd.ProductName, Amount: cmd.Amount,
		Price: cmd.Price, PriceCharge: priceCharge, ProductCode: cmd.ProductCode,
		DiscountEnabled: cmd.EnableDiscount, Discount: cmd.Discount, SysCreationDate: &now,
	}
	ob := s.createOutbox(trx.TransactionID, "servicelogs", map[string]any{"op": "createOrder", "txId": trx.TransactionID})
	if err := s.store.InsertTxWithOutbox(ctx, trx, ob, idemKey); err != nil {
		if idemKey != "" && errors.Is(err, store.ErrIdempotencyExists) {
			for i := 0; i < 5; i++ {
				if existing := cachedTrx(ctx, s.store, idemKey); existing != nil && existing.TransactionID != "" {
					return existing, nil
				}
				time.Sleep(time.Duration(i+1) * 10 * time.Millisecond)
			}
		}
		return nil, errConflict("duplicate transaction or idempotency conflict — retry")
	}
	if idemKey != "" {
		b, _ := json.Marshal(trx)
		_ = s.store.PutIdempotency(ctx, idemKey, string(b))
	}
	return &trx, nil
}

// Charge — idempotent via TransactionID. CREATED→READY.
func (s *Service) Charge(ctx context.Context, transactionID string) (*ChargeRs, error) {
	trx, err := s.findTx(ctx, transactionID)
	if err != nil {
		return nil, err
	}
	if trx.PaymentStatus == StatusReady {
		return &ChargeRs{ID: "pgr_" + trx.TransactionID, TransactionID: trx.TransactionID, Status: StatusReady, CheckoutURL: "/pay/" + trx.TransactionID}, nil
	}
	nxt, err := validateTransition(trx.PaymentStatus, "charge")
	if err != nil {
		return nil, err
	}
	ob := s.createOutbox(transactionID, "servicelogs", map[string]any{"op": "charge", "txId": transactionID, "next": nxt})
	if err := s.store.UpdateStatusWithOutbox(ctx, transactionID, StatusReady, nxt, ob); err != nil {
		return nil, errConflict("failed to transition to READY")
	}
	return &ChargeRs{ID: "pgr_" + transactionID, TransactionID: transactionID, Status: nxt, CheckoutURL: "/pay/" + transactionID}, nil
}

// OnCallback — READY→SUCCESS/FAILED + invoice outbox atomically.
func (s *Service) OnCallback(ctx context.Context, evt CallbackEvt) error {
	if evt.ReferenceID == "" || (evt.Status != "SUCCEEDED" && evt.Status != "FAILED") {
		return errBadRequest("invalid callback")
	}
	trx, err := s.findTx(ctx, evt.ReferenceID)
	if err != nil {
		return err
	}
	if trx.PaymentStatus == StatusSuccess || trx.PaymentStatus == StatusPublished {
		return nil
	}
	event, nxt, topic := "callback_success", StatusSuccess, "ms-notify-payment"
	if evt.Status == StatusFailed {
		event, nxt, topic = "callback_failed", StatusFailed, "servicelogs"
	}
	if _, err := validateTransition(trx.PaymentStatus, event); err != nil {
		return err
	}
	ob := s.createOutbox(evt.ReferenceID, topic, map[string]any{"transactionId": evt.ReferenceID, "status": nxt})
	orderStatus := StatusPublished
	if nxt == StatusFailed {
		orderStatus = trx.OrderStatus
	}
	return s.store.UpdateStatusWithOutbox(ctx, evt.ReferenceID, orderStatus, nxt, ob)
}

// Refund — SUCCESS/PUBLISHED→REFUND, full amount only.
func (s *Service) Refund(ctx context.Context, transactionID string) (*RefundRs, error) {
	trx, err := s.findTx(ctx, transactionID)
	if err != nil {
		return nil, err
	}
	nxt, err := validateTransition(trx.PaymentStatus, "refund")
	if err != nil {
		return nil, errConflict("charge is not in a refundable state")
	}
	ob := s.createOutbox(transactionID, "ms-notify-payment", map[string]any{"transactionId": transactionID, "op": "refund", "amount": trx.PriceCharge})
	if err := s.store.UpdateStatusWithOutbox(ctx, transactionID, trx.OrderStatus, nxt, ob); err != nil {
		return nil, errConflict("refund transition failed")
	}
	return &RefundRs{ID: "pgr_" + uuid.NewString(), TransactionID: transactionID, Status: nxt}, nil
}

func (s *Service) createOutbox(txID, topic string, payload any) *store.Outbox {
	b, _ := json.Marshal(payload)
	return &store.Outbox{ID: uuid.NewString(), AggregateID: txID, Topic: topic, Payload: b, CreatedAt: time.Now()}
}
