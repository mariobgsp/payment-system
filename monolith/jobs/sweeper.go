package jobs

import (
	"context"
	"encoding/json"
	"time"

	"payment-system/monolith/store"

	"github.com/google/uuid"
)

// Reconcile sweeps stale READY rows → FAILED with outbox.
// Returns count of rows transitioned. Uses Store.FindStaleReady + UpdateStatusWithOutbox.
// ponytail: batch limit 100, loop till empty; switch to single UPDATE ... RETURNING if pg load matters.
func Reconcile(ctx context.Context, s store.Store, staleSince time.Duration) int {
	before := time.Now().Add(-staleSince)
	total := 0
	for {
		batch, err := s.FindStaleReady(ctx, before, 100)
		if err != nil || len(batch) == 0 {
			break
		}
		for _, trx := range batch {
			// idempotent: only READY should be returned by FindStaleReady, but guard
			if trx.PaymentStatus != "READY" {
				continue
			}
			payload, _ := json.Marshal(map[string]string{
				"transactionId": trx.TransactionID,
				"oldStatus":     trx.PaymentStatus,
				"newStatus":     "FAILED",
				"reason":        "stale-ready-timeout",
			})
			ob := &store.Outbox{
				ID:          uuid.NewString(),
				AggregateID: trx.TransactionID,
				Topic:       "servicelogs",
				Payload:     payload,
				CreatedAt:   time.Now(),
			}
			if err := s.UpdateStatusWithOutbox(ctx, trx.TransactionID, trx.OrderStatus, "FAILED", ob); err != nil {
				continue
			}
			total++
		}
		if len(batch) < 100 {
			break
		}
	}
	return total
}
