package jobs

import (
	"context"
	"errors"
	"testing"

	"payment-system/monolith/store"
)

type errSender struct{}

func (errSender) Send(_ context.Context, _ string, _ []byte) error {
	return errors.New("boom")
}

type memDLQ struct{ s store.Store }

func (m memDLQ) InsertDLQ(ctx context.Context, ob store.Outbox, lastErr string) error {
	return m.s.InsertOutbox(ctx, &store.Outbox{
		ID: ob.ID + "-dlq", AggregateID: ob.AggregateID,
		Topic: "dlq." + ob.Topic, Payload: []byte(lastErr),
	})
}

// Phase 3 gate: poison outbox row → 3x retry then DLQ, never infinite loop.
func TestPoller_PoisonGoesToDLQ(t *testing.T) {
	ctx := context.Background()
	mem := store.NewMemoryStore()
	_ = mem.InsertOutbox(ctx, &store.Outbox{ID: "ob1", AggregateID: "tx1", Topic: "ms-notify-payment", Payload: []byte(`{}`)})
	p := &Poller{Store: mem, Send: errSender{}, DLQStore: memDLQ{mem}}
	processed, dlq := p.Tick(ctx)
	if processed != 0 || dlq != 1 {
		t.Fatalf("got processed=%d dlq=%d want 0,1", processed, dlq)
	}
	// poison marked processed → next tick is clean
	processed2, dlq2 := p.Tick(ctx)
	if processed2 != 0 || dlq2 != 0 {
		t.Fatalf("second tick got %d,%d want 0,0 (only DLQ row left, still unprocessed)", processed2, dlq2)
	}
}
