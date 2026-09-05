package jobs

import (
	"context"
	"strings"
	"time"

	"payment-system/monolith/store"
)

// Sender is transport adapter at outbox seam — prod = http/kafka, test = fake.
// Two adapters = real seam per deepen doc.
type Sender interface {
	Send(ctx context.Context, topic string, payload []byte) error
}

// Poller processes unprocessed outbox rows with retry 3x exponential backoff + DLQ.
// ponytail: DLQ is same outbox table with topic=dlq.<orig>; split to dedicated table if volume high.
type Poller struct {
	Store    store.Store
	Send     Sender
	DLQStore DLQStore // optional, nil → log and mark processed to avoid poison
}

type DLQStore interface {
	InsertDLQ(ctx context.Context, ob store.Outbox, lastErr string) error
}

func (p *Poller) Tick(ctx context.Context) (processed, dlq int) {
	batch, err := p.Store.ListUnprocessedOutbox(ctx, 50)
	if err != nil || len(batch) == 0 {
		return 0, 0
	}
	for _, ob := range batch {
		if strings.HasPrefix(ob.Topic, "dlq.") {
			continue // terminal DLQ rows are never redelivered
		}
		if err := p.sendWithRetry(ctx, ob); err != nil {
			if p.DLQStore != nil {
				_ = p.DLQStore.InsertDLQ(ctx, ob, err.Error())
			}
			// mark processed to avoid infinite poison loop — DLQ holds failure
			_ = p.Store.MarkOutboxProcessed(ctx, ob.ID)
			dlq++
			continue
		}
		_ = p.Store.MarkOutboxProcessed(ctx, ob.ID)
		processed++
	}
	return processed, dlq
}

func (p *Poller) sendWithRetry(ctx context.Context, ob store.Outbox) error {
	backoffs := []time.Duration{100 * time.Millisecond, 400 * time.Millisecond, 1600 * time.Millisecond}
	var lastErr error
	for i := 0; i < 3; i++ {
		// ponytail: context-aware sleep, per-call timeout handled by Sender
		if err := p.Send.Send(ctx, ob.Topic, ob.Payload); err == nil {
			return nil
		} else {
			lastErr = err
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

// Loop runs till ctx done — caller owns ticker.
func (p *Poller) Loop(ctx context.Context, interval time.Duration) {
	t := time.NewTicker(interval)
	defer t.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-t.C:
			p.Tick(ctx)
		}
	}
}
