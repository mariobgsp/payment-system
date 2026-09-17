package usecase

import (
	"context"
	"fmt"
	"log"
	"time"

	"github.com/go-resty/resty/v2"
)

// Notifier seam — transport for callbacks
type Notifier interface {
	Send(ctx context.Context, payload []byte, signature, timestamp string) error
}

// backoffs is the single retry schedule — was duplicated in HttpNotifier + jobs.Poller.
var backoffs = []time.Duration{100 * time.Millisecond, 400 * time.Millisecond, 1600 * time.Millisecond}

// HttpNotifier prod adapter — resty 15s timeout 3x backoff
type HttpNotifier struct {
	url    string
	secret string
	client *resty.Client
}

func NewHttpNotifier(url, secret string) *HttpNotifier {
	return &HttpNotifier{url: url, secret: secret, client: resty.New().SetTimeout(15 * time.Second)}
}
func (h *HttpNotifier) Send(ctx context.Context, payload []byte, signature, timestamp string) error {
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
		if i < 2 {
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
