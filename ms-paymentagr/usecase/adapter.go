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
	"paymentagr/config"
	"paymentagr/models"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/go-resty/resty/v2"
	"github.com/google/uuid"
	redisDriver "github.com/redis/go-redis/v9"
)

// ponytail: fixed 5m TTL (300s) via config.RedisTTL; per-method TTL if throughput matters

var referenceIdPattern = regexp.MustCompile(`^[A-Za-z0-9\-_.]{3,64}$`)

// ChargeCmd / RefundCmd aliases for hybrid Interface readability
type ChargeCmd = models.ChargeRq
type RefundCmd = models.RefundRq

// Store seam — where behavior can be altered without editing in place
type Store interface {
	Set(ctx context.Context, key string, val []byte, ttl time.Duration) error
	Get(ctx context.Context, key string) (string, error)
}

// Notifier seam — transport for callbacks
type Notifier interface {
	Send(ctx context.Context, payload []byte, signature, timestamp string) error
}

// Adapter is PaymentAdapter deep Module — small Interface, substantial hidden behavior
// Hides validation, TTL, signing, retry, referenceIdPattern
type Adapter struct {
	cfg      *config.Config
	store    Store
	notifier Notifier
}

// NewAdapter constructor injection — replaces global sync.Once
func NewAdapter(cfg *config.Config, store Store, notifier Notifier) *Adapter {
	return &Adapter{cfg: cfg, store: store, notifier: notifier}
}

// Config exposes cfg for middleware (replaces global Config())
func (a *Adapter) Config() *config.Config { return a.cfg }

// Store accessor for HealthCheck
func (a *Adapter) Store() Store { return a.store }

// RedisStore prod adapter
type RedisStore struct {
	client *redisDriver.Client
}

func NewRedisStore(cfg *config.Config) *RedisStore {
	return &RedisStore{client: redisDriver.NewClient(&redisDriver.Options{
		Addr:     cfg.RedisHost,
		Password: cfg.RedisPassword,
		DB:       0,
	})}
}
func (r *RedisStore) Set(ctx context.Context, key string, val []byte, ttl time.Duration) error {
	return r.client.Set(ctx, key, val, ttl).Err()
}
func (r *RedisStore) Get(ctx context.Context, key string) (string, error) {
	return r.client.Get(ctx, key).Result()
}
func (r *RedisStore) Ping(ctx context.Context) error {
	_, err := r.client.Ping(ctx).Result()
	return err
}
func (r *RedisStore) Client() *redisDriver.Client { return r.client }

// MemoryStore fake adapter for tests
type MemoryStore struct {
	data map[string]string
}

func NewMemoryStore() *MemoryStore {
	return &MemoryStore{data: make(map[string]string)}
}
func (m *MemoryStore) Set(_ context.Context, key string, val []byte, ttl time.Duration) error {
	m.data[key] = string(val)
	return nil
}
func (m *MemoryStore) Get(_ context.Context, key string) (string, error) {
	v, ok := m.data[key]
	if !ok {
		return "", errors.New("not found")
	}
	return v, nil
}

// HttpNotifier prod adapter — resty 15s timeout 3x backoff
type HttpNotifier struct {
	url    string
	secret string
	client *resty.Client
}

func NewHttpNotifier(url, secret string) *HttpNotifier {
	c := resty.New().SetTimeout(15 * time.Second)
	return &HttpNotifier{url: url, secret: secret, client: c}
}
func (h *HttpNotifier) Send(ctx context.Context, payload []byte, signature, timestamp string) error {
	// signature/timestamp already computed by caller; this adapter just POSTs
	// 3x backoff ponytail: fixed backoffs 100ms,400ms,1600ms; per-status retry if needed
	backoffs := []time.Duration{100 * time.Millisecond, 400 * time.Millisecond, 1600 * time.Millisecond}
	var lastErr error
	for i := 0; i < 3; i++ {
		resp, err := h.client.R().
			SetContext(ctx).
			SetHeader("Content-Type", "application/json").
			SetHeader("x-callback-signature", signature).
			SetHeader("x-callback-timestamp", timestamp).
			SetBody(payload).
			Post(h.url)
		if err == nil && resp.IsSuccess() {
			log.Printf("notify response status: %d, body: %s", resp.StatusCode(), resp.String())
			return nil
		}
		if err != nil {
			lastErr = err
		} else {
			lastErr = fmt.Errorf("notify failed status %d: %s", resp.StatusCode(), resp.String())
		}
		if i < len(backoffs)-1 {
			select {
			case <-ctx.Done():
				return ctx.Err()
			case <-time.After(backoffs[i]):
			}
		}
	}
	return lastErr
}

// FakeNotifier test adapter — records
type FakeNotifier struct {
	Sent []FakeSent
}
type FakeSent struct {
	Payload   []byte
	Signature string
	Timestamp string
}

func (f *FakeNotifier) Send(_ context.Context, payload []byte, signature, timestamp string) error {
	f.Sent = append(f.Sent, FakeSent{Payload: payload, Signature: signature, Timestamp: timestamp})
	return nil
}

// signPayload hidden behind seam (was SignPayload global)
func (a *Adapter) signPayload(secret, timestamp string, payload []byte) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(timestamp))
	mac.Write(payload)
	return hex.EncodeToString(mac.Sum(nil))
}

// SignPayload exported for tests (wraps adapter with default)
func SignPayload(secret, timestamp string, payload []byte) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(timestamp))
	mac.Write(payload)
	return hex.EncodeToString(mac.Sum(nil))
}

func (a *Adapter) validateCharge(rq *models.ChargeRq) error {
	if rq.ReferenceId == "" || !referenceIdPattern.MatchString(rq.ReferenceId) {
		return errors.New("invalid or missing reference_id")
	}
	if rq.Amount <= 0 || rq.Amount > 1000000000 {
		return errors.New("invalid amount, must be between 1 and 1000000000")
	}
	if rq.Currency == "" {
		return errors.New("missing currency")
	}
	if rq.PaymentCode == "" {
		return errors.New("missing payment_code")
	}
	if rq.CheckoutMethod == "" {
		return errors.New("missing checkout_method")
	}
	return nil
}

// Charge hybrid Interface — small Interface, idempotency via idemKey (refID natural key if empty)
func (a *Adapter) Charge(ctx context.Context, refID string, cmd ChargeCmd, idemKey string) (*models.ChargeRs, error) {
	// idemKey uses refID if empty — ponytail: fixed 5m TTL idempotency
	if refID == "" {
		refID = cmd.ReferenceId
	}
	if refID == "" || !referenceIdPattern.MatchString(refID) {
		return nil, models.NewBadRequest("invalid or missing reference_id")
	}
	// ensure cmd ReferenceId matches refID
	cmd.ReferenceId = refID
	if err := a.validateCharge(&cmd); err != nil {
		return nil, models.NewBadRequest(err.Error())
	}
	// idempotency check — if already stored, return cached
	if idemKey == "" {
		idemKey = "chargetrx_" + refID
	}
	if existing, err := a.store.Get(ctx, idemKey); err == nil && existing != "" {
		// try to unmarshal as ChargeRs
		var cached models.ChargeRs
		if err := json.Unmarshal([]byte(existing), &cached); err == nil && cached.ReferenceId == refID {
			return &cached, nil
		}
	}

	checkoutUrl := fmt.Sprintf("%s/payments/redirect/%s", a.cfg.PublicBaseURL, refID)
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
		Action:         &models.Action{CheckoutUrl: checkoutUrl},
	}
	bodyPayload, err := json.Marshal(res)
	if err != nil {
		log.Print("failed parsing json request", res, err)
		return nil, models.NewInternalError("failed to process charge")
	}
	key := "chargetrx_" + refID
	ttl := time.Duration(a.cfg.RedisTTL) * time.Second
	if err := a.store.Set(ctx, key, bodyPayload, ttl); err != nil {
		log.Print("set data to redis failed", err)
		return nil, models.NewInternalError("failed to store charge")
	}
	// also store under idemKey if different
	if idemKey != key {
		_ = a.store.Set(ctx, idemKey, bodyPayload, ttl)
	}
	return res, nil
}

// Refund hybrid Interface
func (a *Adapter) Refund(ctx context.Context, refID string, cmd RefundCmd) (*models.RefundRs, error) {
	if refID == "" {
		refID = cmd.ReferenceId
	}
	if refID == "" || !referenceIdPattern.MatchString(refID) {
		return nil, models.NewBadRequest("invalid or missing reference_id")
	}
	if cmd.Amount <= 0 {
		return nil, models.NewBadRequest("invalid amount, must be greater than 0")
	}
	if cmd.Currency == "" {
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
	// store refund
	b, _ := json.Marshal(res)
	if err := a.store.Set(ctx, "refundtrx_"+refID, b, time.Duration(a.cfg.RedisTTL)*time.Second); err != nil {
		log.Print("set refund data to redis failed", err)
		return nil, models.NewInternalError("failed to store refund")
	}
	// trigger callback async with context
	go func() {
		// use background with timeout for callback
		cctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		_ = a.TriggerCallback(cctx, "refundtrx_"+refID)
	}()
	return res, nil
}

// Redirect hybrid Interface
func (a *Adapter) Redirect(ctx context.Context, refID string) (*models.SimpleResponse, error) {
	if refID == "" || !referenceIdPattern.MatchString(refID) {
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

func (a *Adapter) GetCharge(ctx context.Context, refId string) (*models.ChargeRs, error) {
	data, err := a.store.Get(ctx, "chargetrx_"+refId)
	if err != nil {
		return nil, err
	}
	var charge models.ChargeRs
	if err := json.Unmarshal([]byte(data), &charge); err != nil {
		return nil, err
	}
	return &charge, nil
}

// TriggerCallback copies charge, updates status, persists back to Redis, then notifies — fixes lost update bug
func (a *Adapter) TriggerCallback(ctx context.Context, opKey string) error {
	parts := strings.SplitN(opKey, "_", 2)
	if len(parts) < 2 {
		return errors.New("invalid opKey")
	}
	charge, err := a.GetCharge(ctx, parts[1])
	if err != nil {
		log.Printf("callback skipped, charge not found: %v", err)
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
		log.Print("failed parsing callback payload", err)
		return err
	}
	// persist updated status back to Redis before callback — fixes stale PENDING replay
	ttl := time.Duration(a.cfg.RedisTTL) * time.Second
	if err := a.store.Set(ctx, "chargetrx_"+parts[1], payload, ttl); err != nil {
		log.Printf("failed to persist callback status: %v", err)
		return err
	}
	return a.sendCallback(ctx, payload)
}

func (a *Adapter) sendCallback(ctx context.Context, payload []byte) error {
	timestamp := strconv.FormatInt(time.Now().Unix(), 10)
	signature := a.signPayload(a.cfg.NotifySecret, timestamp, payload)
	return a.notifier.Send(ctx, payload, signature, timestamp)
}
