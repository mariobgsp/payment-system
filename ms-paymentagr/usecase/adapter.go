package usecase

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"regexp"
	"strconv"
	"strings"
	"time"

	"paymentagr/config"
	"paymentagr/models"

	"github.com/google/uuid"
)

// ponytail: fixed 5m TTL via config.RedisTTL; per-method TTL if throughput matters.

var referenceIdPattern = regexp.MustCompile(`^[A-Za-z0-9\-_.]{3,64}$`)

// ChargeCmd / RefundCmd aliases for readability
type ChargeCmd = models.ChargeRq
type RefundCmd = models.RefundRq

// Adapter is the payment deep Module — validation, TTL, signing, retry hidden.
type Adapter struct {
	cfg      *config.Config
	store    Store
	notifier Notifier
}

func NewAdapter(cfg *config.Config, store Store, notifier Notifier) *Adapter {
	return &Adapter{cfg: cfg, store: store, notifier: notifier}
}

func (a *Adapter) Config() *config.Config { return a.cfg }
func (a *Adapter) Store() Store           { return a.store }

func signPayload(secret, timestamp string, payload []byte) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(timestamp))
	mac.Write(payload)
	return hex.EncodeToString(mac.Sum(nil))
}

// SignPayload exported for tests.
func SignPayload(secret, timestamp string, payload []byte) string {
	return signPayload(secret, timestamp, payload)
}

func validRef(ref string) bool { return ref != "" && referenceIdPattern.MatchString(ref) }

func (a *Adapter) validateCharge(rq *models.ChargeRq) error {
	switch {
	case !validRef(rq.ReferenceId):
		return errors.New("invalid or missing reference_id")
	case rq.Amount <= 0 || rq.Amount > 1000000000:
		return errors.New("invalid amount, must be between 1 and 1000000000")
	case rq.Currency == "":
		return errors.New("missing currency")
	case rq.PaymentCode == "":
		return errors.New("missing payment_code")
	case rq.CheckoutMethod == "":
		return errors.New("missing checkout_method")
	}
	return nil
}

// Charge — idempotent via refID natural key.
func (a *Adapter) Charge(ctx context.Context, refID string, cmd ChargeCmd, idemKey string) (*models.ChargeRs, error) {
	if refID == "" {
		refID = cmd.ReferenceId
	}
	if !validRef(refID) {
		return nil, models.NewBadRequest("invalid or missing reference_id")
	}
	cmd.ReferenceId = refID
	if err := a.validateCharge(&cmd); err != nil {
		return nil, models.NewBadRequest(err.Error())
	}
	if idemKey == "" {
		idemKey = "chargetrx_" + refID
	}
	if existing, err := a.store.Get(ctx, idemKey); err == nil && existing != "" {
		var cached models.ChargeRs
		if err := json.Unmarshal([]byte(existing), &cached); err == nil && cached.ReferenceId == refID {
			return &cached, nil
		}
	}

	res := &models.ChargeRs{
		Id:             "pgr_" + uuid.New().String(),
		ReferenceId:    refID,
		Status:         "PENDING",
		Currency:       strings.ToUpper(cmd.Currency),
		CheckoutMethod: cmd.CheckoutMethod,
		Amount:         cmd.Amount,
		PaymentCode:    cmd.PaymentCode,
		RedirectUrl:    cmd.RedirectUrl,
		CallbackUrl:    cmd.CallbackUrl,
		Created:        time.Now(),
		Updated:        time.Now(),
		Action:         &models.Action{CheckoutUrl: fmt.Sprintf("%s/payments/redirect/%s", a.cfg.PublicBaseURL, refID)},
	}
	body, err := json.Marshal(res)
	if err != nil {
		return nil, models.NewInternalError("failed to process charge")
	}
	ttl := time.Duration(a.cfg.RedisTTL) * time.Second
	if err := a.store.Set(ctx, "chargetrx_"+refID, body, ttl); err != nil {
		log.Print("set data to redis failed", err)
		return nil, models.NewInternalError("failed to store charge")
	}
	if idemKey != "chargetrx_"+refID {
		_ = a.store.Set(ctx, idemKey, body, ttl)
	}
	return res, nil
}

func (a *Adapter) Refund(ctx context.Context, refID string, cmd RefundCmd) (*models.RefundRs, error) {
	if refID == "" {
		refID = cmd.ReferenceId
	}
	switch {
	case !validRef(refID):
		return nil, models.NewBadRequest("invalid or missing reference_id")
	case cmd.Amount <= 0:
		return nil, models.NewBadRequest("invalid amount, must be greater than 0")
	case cmd.Currency == "":
		return nil, models.NewBadRequest("missing currency")
	}
	charge, err := a.GetCharge(ctx, refID)
	if err != nil {
		return nil, models.NewNotFound("charge not found for reference_id")
	}
	if charge.Status != "SUCCEEDED" {
		return nil, models.NewBadRequest("charge is not in a refundable state")
	}
	res := &models.RefundRs{
		Id:           "pgr_" + uuid.New().String(),
		ReferenceId:  refID,
		Amount:       cmd.Amount,
		Reason:       cmd.Reason,
		RefundStatus: "REFUND-PENDING",
		Currency:     strings.ToUpper(cmd.Currency),
		CreatedAt:    time.Now(),
		UpdatedAt:    time.Now(),
	}
	b, _ := json.Marshal(res)
	if err := a.store.Set(ctx, "refundtrx_"+refID, b, time.Duration(a.cfg.RedisTTL)*time.Second); err != nil {
		return nil, models.NewInternalError("failed to store refund")
	}
	go func() {
		cctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		_ = a.TriggerCallback(cctx, "refundtrx_"+refID)
	}()
	return res, nil
}

func (a *Adapter) Redirect(ctx context.Context, refID string) (*models.SimpleResponse, error) {
	if !validRef(refID) {
		return nil, models.NewBadRequest("invalid or missing transaction id")
	}
	charge, err := a.GetCharge(ctx, refID)
	if err != nil {
		return nil, models.NewNotFound("charge not found")
	}
	if charge.Status == "SUCCEEDED" {
		return &models.SimpleResponse{Status: "Ok", Code: "00", Message: "Payment already confirmed"}, nil
	}
	go func() {
		cctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		_ = a.TriggerCallback(cctx, "chargetrx_"+refID)
	}()
	return &models.SimpleResponse{Status: "Ok", Code: "00", Message: "Request being processed"}, nil
}

func (a *Adapter) GetCharge(ctx context.Context, refID string) (*models.ChargeRs, error) {
	data, err := a.store.Get(ctx, "chargetrx_"+refID)
	if err != nil {
		return nil, err
	}
	var charge models.ChargeRs
	if err := json.Unmarshal([]byte(data), &charge); err != nil {
		return nil, err
	}
	return &charge, nil
}

// TriggerCallback persists SUCCEEDED status then notifies — fixes lost-update bug.
func (a *Adapter) TriggerCallback(ctx context.Context, opKey string) error {
	parts := strings.SplitN(opKey, "_", 2)
	if len(parts) < 2 {
		return errors.New("invalid opKey")
	}
	charge, err := a.GetCharge(ctx, parts[1])
	if err != nil {
		return err
	}
	updated := *charge
	switch parts[0] {
	case "chargetrx":
		updated.Status = "SUCCEEDED"
	case "refundtrx":
		updated.Status = "SUCCESS-REFUND"
	default:
		return errors.New("unknown op")
	}
	updated.Updated = time.Now()
	payload, err := json.Marshal(updated)
	if err != nil {
		return err
	}
	if err := a.store.Set(ctx, "chargetrx_"+parts[1], payload, time.Duration(a.cfg.RedisTTL)*time.Second); err != nil {
		return err
	}
	return a.sendCallback(ctx, payload)
}

func (a *Adapter) sendCallback(ctx context.Context, payload []byte) error {
	ts := strconv.FormatInt(time.Now().Unix(), 10)
	return a.notifier.Send(ctx, payload, signPayload(a.cfg.NotifySecret, ts, payload), ts)
}
