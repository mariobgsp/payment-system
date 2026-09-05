package usecase

import (
	"context"
	"encoding/json"
	"sync"
	"time"

	"paymentagr/config"
	"paymentagr/models"

	redisDriver "github.com/redis/go-redis/v9"
)

var (
	defaultOnce    sync.Once
	defaultAdapter *Adapter
)

func defaultAdapterInit() *Adapter {
	defaultOnce.Do(func() {
		cfg := config.Load()
		store := NewRedisStore(cfg)
		notifier := NewHttpNotifier(cfg.NotifyURL, cfg.NotifySecret)
		defaultAdapter = NewAdapter(cfg, store, notifier)
	})
	return defaultAdapter
}

// Backward compat wrappers — keep old call sites working, now delegate to Adapter with context
func Config() *config.Config { return defaultAdapterInit().Config() }
func Redis() *redisDriver.Client {
	if rs, ok := defaultAdapterInit().Store().(*RedisStore); ok {
		return rs.Client()
	}
	return nil
}
func SetData(key string, data interface{}, ttl time.Duration) error {
	var b []byte
	switch v := data.(type) {
	case []byte:
		b = v
	case string:
		b = []byte(v)
	default:
		jb, err := json.Marshal(v)
		if err != nil {
			return err
		}
		b = jb
	}
	return defaultAdapterInit().Store().Set(context.Background(), key, b, ttl)
}
func GetData(key string) (string, error) {
	return defaultAdapterInit().Store().Get(context.Background(), key)
}
func GetCharge(refId string) (*models.ChargeRs, error) {
	return defaultAdapterInit().GetCharge(context.Background(), refId)
}
func TriggerCallback(opKey string) {
	_ = defaultAdapterInit().TriggerCallback(context.Background(), opKey)
}
func ChargePayment(rq *models.ChargeRq) (*models.ChargeRs, error) {
	return defaultAdapterInit().Charge(context.Background(), rq.ReferenceId, *rq, "")
}
func RefundPayment(rq *models.RefundRq) (*models.RefundRs, error) {
	return defaultAdapterInit().Refund(context.Background(), rq.ReferenceId, *rq)
}
func RedirectPayment(trxid string) (*models.SimpleResponse, error) {
	return defaultAdapterInit().Redirect(context.Background(), trxid)
}
