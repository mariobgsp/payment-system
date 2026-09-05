package platform

import (
	"context"
	"net/http"
	"testing"
	"time"

	"payment-system/monolith/lifecycle"
	"payment-system/monolith/store"
)

func TestRequest_Ok_Fail(t *testing.T) {
	mem := store.NewMemoryStore()
	p := &Platform{Store: mem, Now: func() time.Time { return time.Date(2026, 8, 25, 12, 0, 0, 0, time.FixedZone("WIB", 7*3600)) }}
	h := http.Header{}
	h.Set("x-request-id", "req-123")
	req := p.Request(RequestOpts{OpName: "test-op", Payload: map[string]string{"a": "b"}, R: &http.Request{Header: h, Method: "GET", RemoteAddr: "1.2.3.4"}})
	if req.RequestID != "req-123" {
		t.Fatalf("want req-123 got %s", req.RequestID)
	}
	if req.Channel != "WEB" {
		t.Fatalf("want WEB got %s", req.Channel)
	}
	resp := p.Ok(req, map[string]string{"ok": "1"})
	if resp.Status != 200 {
		t.Fatalf("want 200")
	}
	if resp.Headers["x-request-id"] != "req-123" {
		t.Fatal("header missing")
	}
	ae := lifecycle.NewBadRequest("bad")
	resp2 := p.Fail(req, ae)
	if resp2.Status != 400 {
		t.Fatalf("want 400 got %d", resp2.Status)
	}
	// generic error → 500
	resp3 := p.Fail(req, &testErr{"boom"})
	if resp3.Status != 500 {
		t.Fatal("want 500")
	}
	// publish inserts outbox
	if err := p.Publish(context.Background(), req, resp); err != nil {
		t.Fatalf("publish %v", err)
	}
	list, _ := mem.ListUnprocessedOutbox(context.Background(), 10)
	if len(list) == 0 {
		t.Fatal("want outbox")
	}
}

type testErr struct{ m string }

func (e *testErr) Error() string { return e.m }

func TestRequest_Jakarta(t *testing.T) {
	p := &Platform{Now: func() time.Time { return time.Date(2026, 8, 25, 10, 0, 0, 0, time.UTC) }}
	req := p.Request(RequestOpts{OpName: "op", R: &http.Request{Header: http.Header{}}})
	// Jakarta is UTC+7, so 10:00 UTC = 17:00 Jakarta
	if req.RequestAt != "2026-08-25 17:00:00" {
		t.Fatalf("want Jakarta time got %s", req.RequestAt)
	}
}
