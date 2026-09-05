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

// ApiError — Interface part callers must know (code, message, status mapping).
type ApiError struct {
	Code    int
	Message string
}

func (e *ApiError) Error() string { return e.Message }

func errBadRequest(msg string) *ApiError   { return &ApiError{Code: 400, Message: msg} }
func errNotFound(msg string) *ApiError     { return &ApiError{Code: 404, Message: msg} }
func errConflict(msg string) *ApiError     { return &ApiError{Code: 409, Message: msg} }
func errUnauthorized(msg string) *ApiError { return &ApiError{Code: 401, Message: msg} }

// Exported for handlers — Interface part.
func NewBadRequest(msg string) *ApiError   { return errBadRequest(msg) }
func NewNotFound(msg string) *ApiError     { return errNotFound(msg) }
func NewConflict(msg string) *ApiError     { return errConflict(msg) }
func NewUnauthorized(msg string) *ApiError { return errUnauthorized(msg) }

// Service is TransactionLifecycle deep Module seam.
// Small Interface, substantial hidden behaviour (pricing, state chart, idempotency, authz, outbox).
type Service struct {
	store Store
}

// Store aliases internal seam for test injectability (Local-substitutable).
type Store = store.Store

func New(s Store) *Service      { return &Service{store: s} }
func (s *Service) Store() Store { return s.store }

// Status constants — G25: replace magic numbers/strings
const (
	StatusCreated   = "CREATED"
	StatusReady     = "READY"
	StatusSuccess   = "SUCCESS"
	StatusPublished = "PUBLISHED"
	StatusFailed    = "FAILED"
	StatusRefund    = "REFUND"
)

// Primitive Obsession: wrap transaction/idempotency primitives for type safety
type TransactionID string
type IdempotencyKey string

// Data Clumps: bundle product/user refs
type ProductRef struct {
	Code, Name     string
	Price          int64
	Discount       float64
	EnableDiscount bool
}
type UserRef struct {
	Username, UserID string
}

// --- Interface types ---

type CreateCmd struct {
	Username       string
	UserID         string
	ProductCode    string
	ProductName    string
	Price          int64
	Amount         int64
	EnableDiscount bool
	Discount       float64 // from Product row
}

type ChargeRs struct {
	ID            string `json:"id"`
	TransactionID string `json:"transactionId"`
	CheckoutURL   string `json:"checkoutUrl"`
	Status        string `json:"status"`
}

type CallbackEvt struct {
	ReferenceID string `json:"referenceId"` // transactionId
	Status      string `json:"status"`      // SUCCEEDED / FAILED
}

type RefundRs struct {
	ID            string `json:"id"`
	TransactionID string `json:"transactionId"`
	Status        string `json:"status"` // REFUND
}

// state chart — uses named constants, G25 fix
var transitions = map[string]map[string]string{
	StatusCreated:   {"charge": StatusReady},
	StatusReady:     {"callback_success": StatusSuccess, "callback_failed": StatusFailed, "timeout": StatusFailed},
	StatusSuccess:   {"refund": StatusRefund},
	StatusPublished: {"refund": StatusRefund},
}

func validateTransition(old, event string) (string, error) {
	m, ok := transitions[old]
	if !ok {
		return "", errConflict(fmt.Sprintf("no transitions from %s", old))
	}
	nxt, ok := m[event]
	if !ok {
		return "", errConflict(fmt.Sprintf("invalid event %s from %s", event, old))
	}
	return nxt, nil
}

// Check verifies ownership — authz at seam.
func (s *Service) Check(ctx context.Context, txId, callerUserId string) (*store.ProductTrx, error) {
	trx, err := s.store.FindTxForUpdate(ctx, txId)
	if err != nil {
		return nil, errNotFound("transaction not found")
	}
	if trx == nil {
		return nil, errNotFound("transaction not found")
	}
	if trx.UserID != callerUserId {
		return nil, errUnauthorized("unauthorized: transaction does not belong to caller")
	}
	return trx, nil
}

// CreateOrderWithIdempotency — idempotent via Idempotency-Key header. Pricing inside Lifecycle (deep).
// Stores ProductTrx CREATED + outbox(servicelogs) atomically. N7: name describes side-effect (insert + idempotency).
func (s *Service) CreateOrder(ctx context.Context, cmd CreateCmd, idemKey string) (*store.ProductTrx, error) {
	return s.CreateOrderWithIdempotency(ctx, cmd, IdempotencyKey(idemKey))
}
func (s *Service) CreateOrderWithIdempotency(ctx context.Context, cmd CreateCmd, idemKey IdempotencyKey) (*store.ProductTrx, error) {
	keyStr := string(idemKey)
	if keyStr != "" {
		if cached, _ := s.store.GetIdempotency(ctx, keyStr); cached != "" && cached != "{}" {
			var trx store.ProductTrx
			if err := json.Unmarshal([]byte(cached), &trx); err == nil {
				return &trx, nil
			}
		}
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
		ID:              "MSO-" + uuid.NewString(),
		TransactionID:   "PTRX-" + uuid.NewString()[:8],
		OrderStatus:     StatusCreated,
		PaymentStatus:   StatusCreated,
		UserID:          cmd.UserID,
		ProductName:     cmd.ProductName,
		Amount:          cmd.Amount,
		Price:           cmd.Price,
		PriceCharge:     priceCharge,
		ProductCode:     cmd.ProductCode,
		DiscountEnabled: cmd.EnableDiscount,
		Discount:        cmd.Discount,
		SysCreationDate: &now,
	}
	ob := s.createOutbox(trx.TransactionID, "servicelogs", map[string]any{"op": "createOrder", "txId": trx.TransactionID})
	if err := s.store.InsertTxWithOutbox(ctx, trx, ob, keyStr); err != nil {
		if keyStr != "" && errors.Is(err, store.ErrIdempotencyExists) {
			for i := 0; i < 5; i++ {
				if cached, _ := s.store.GetIdempotency(ctx, keyStr); cached != "" && cached != "{}" {
					var existing store.ProductTrx
					if err2 := json.Unmarshal([]byte(cached), &existing); err2 == nil {
						return &existing, nil
					}
				}
				time.Sleep(time.Duration(i+1) * 10 * time.Millisecond)
			}
			if cached, _ := s.store.GetIdempotency(ctx, keyStr); cached != "" {
				var existing store.ProductTrx
				if err2 := json.Unmarshal([]byte(cached), &existing); err2 == nil && existing.TransactionID != "" {
					return &existing, nil
				}
			}
		}
		return nil, errConflict("duplicate transaction or idempotency conflict — retry")
	}
	if keyStr != "" {
		b, _ := json.Marshal(trx)
		_ = s.store.PutIdempotency(ctx, keyStr, string(b))
	}
	return &trx, nil
}

// Charge — idempotent via TransactionID natural key. CREATED→READY.
func (s *Service) Charge(ctx context.Context, transactionId string) (*ChargeRs, error) {
	return s.ChargeWithID(ctx, TransactionID(transactionId))
}
func (s *Service) ChargeWithID(ctx context.Context, tid TransactionID) (*ChargeRs, error) {
	idStr := string(tid)
	trx, err := s.store.FindTxForUpdate(ctx, idStr)
	if err != nil || trx == nil {
		return nil, errNotFound("transaction not found")
	}
	if trx.PaymentStatus == StatusReady {
		return &ChargeRs{ID: "pgr_" + trx.TransactionID, TransactionID: trx.TransactionID, Status: StatusReady, CheckoutURL: fmt.Sprintf("/pay/%s", trx.TransactionID)}, nil
	}
	nxt, err := validateTransition(trx.PaymentStatus, "charge")
	if err != nil {
		return nil, err
	}
	ob := s.createOutbox(idStr, "servicelogs", map[string]any{"op": "charge", "txId": idStr, "next": nxt})
	if err := s.store.UpdateStatusWithOutbox(ctx, idStr, StatusReady, nxt, ob); err != nil {
		return nil, errConflict("failed to transition to READY")
	}
	return &ChargeRs{ID: "pgr_" + idStr, TransactionID: idStr, Status: nxt, CheckoutURL: fmt.Sprintf("/pay/%s", idStr)}, nil
}

// OnCallback — READY→SUCCESS/FAILED. Writes outbox for invoice (ms-notify-payment) + logs atomically.
func (s *Service) OnCallback(ctx context.Context, evt CallbackEvt) error {
	if evt.ReferenceID == "" || (evt.Status != "SUCCEEDED" && evt.Status != "FAILED") {
		return errBadRequest("invalid callback")
	}
	trx, err := s.store.FindTxForUpdate(ctx, evt.ReferenceID)
	if err != nil || trx == nil {
		return errNotFound("transaction not found")
	}
	if trx.PaymentStatus == StatusSuccess || trx.PaymentStatus == StatusPublished {
		return nil
	}
	event := "callback_success"
	nxt := StatusSuccess
	obTopic := "ms-notify-payment"
	if evt.Status == StatusFailed {
		event = "callback_failed"
		nxt = StatusFailed
		obTopic = "servicelogs"
	}
	if _, err := validateTransition(trx.PaymentStatus, event); err != nil {
		return err
	}
	ob := s.createOutbox(evt.ReferenceID, obTopic, map[string]any{"transactionId": evt.ReferenceID, "status": nxt})
	orderStatus := StatusPublished
	if nxt == StatusFailed {
		orderStatus = trx.OrderStatus
	}
	return s.store.UpdateStatusWithOutbox(ctx, evt.ReferenceID, orderStatus, nxt, ob)
}

// Refund — SUCCESS/PUBLISHED→REFUND, full amount only. ponytail: partial when finance requests.
func (s *Service) Refund(ctx context.Context, transactionId string) (*RefundRs, error) {
	return s.RefundWithID(ctx, TransactionID(transactionId))
}
func (s *Service) RefundWithID(ctx context.Context, tid TransactionID) (*RefundRs, error) {
	idStr := string(tid)
	trx, err := s.store.FindTxForUpdate(ctx, idStr)
	if err != nil || trx == nil {
		return nil, errNotFound("transaction not found")
	}
	nxt, err := validateTransition(trx.PaymentStatus, "refund")
	if err != nil {
		return nil, errConflict("charge is not in a refundable state")
	}
	ob := s.createOutbox(idStr, "ms-notify-payment", map[string]any{"transactionId": idStr, "op": "refund", "amount": trx.PriceCharge})
	if err := s.store.UpdateStatusWithOutbox(ctx, idStr, trx.OrderStatus, nxt, ob); err != nil {
		return nil, errConflict("refund transition failed")
	}
	return &RefundRs{ID: "pgr_" + uuid.NewString(), TransactionID: idStr, Status: nxt}, nil
}

func (s *Service) createOutbox(txId, topic string, payload any) *store.Outbox {
	b, _ := json.Marshal(payload)
	return &store.Outbox{
		ID:          uuid.NewString(),
		AggregateID: txId,
		Topic:       topic,
		Payload:     b,
		CreatedAt:   time.Now(),
	}
}

var _ = errors.New
