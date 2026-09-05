package main

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"testing"
	"time"

	"payment-system/monolith/identity"
	"payment-system/monolith/lifecycle"
	"payment-system/monolith/store"
)

// Phase 1 freeze: legacy compat contract must not drift.
// Frontend points MS_ORDER_URL/MS_PAYMENT_URL at monolith; envelopes below are the frozen deal.
func TestFrozenLegacyEnvelopes(t *testing.T) {
	// writeErr mapping: 409→01, 429→40, 500→99
	cases := []struct {
		err  error
		code int
		want string
	}{
		{lifecycle.NewConflict("x"), 409, "01"},
		{lifecycle.NewBadRequest("x"), 400, "01"},
		{&lifecycle.ApiError{Code: 429, Message: "slow"}, 429, "40"},
	}
	for _, c := range cases {
		rec := httptest.NewRecorder()
		writeErr(rec, c.err)
		if rec.Code != c.code {
			t.Fatalf("status got %d want %d", rec.Code, c.code)
		}
		body := map[string]any{}
		_ = json.NewDecoder(rec.Body).Decode(&body)
		if body["code"] != c.want {
			t.Fatalf("code got %v want %s", body["code"], c.want)
		}
	}

	// handleNotify rejects garbage body with 400 (frozen invalid-callback behavior)
	t.Setenv("NOTIFY_SECRET", "")
	t.Setenv("PARTNER_CHARGE_URL", "")
	svc := lifecycle.New(store.NewMemoryStore())
	rec := httptest.NewRecorder()
	req := httptest.NewRequest("POST", "/ms/api/v1/payment/notify", strings.NewReader("{bad"))
	handleNotify(svc, nil)(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("notify bad body got %d want 400", rec.Code)
	}

	// legacy payment create with empty tx → 404 (frozen: transaction not found, code 01)
	rec2 := httptest.NewRecorder()
	req2 := httptest.NewRequest("POST", "/ms/api/v1/payment/create/SHOPEEPAY", nil)
	handleLegacyPaymentCreate(svc)(rec2, req2)
	if rec2.Code != http.StatusNotFound {
		t.Fatalf("legacy create empty tx got %d want 404", rec2.Code)
	}
}

// Phase 5 gate: PG log query + retention replaces Mongo ServiceLog.
func TestLogsQueryAndRetention(t *testing.T) {
	mem := store.NewMemoryStore()
	ctx := context.Background()
	old := time.Now().Add(-40 * 24 * time.Hour)
	_ = mem.InsertOutbox(ctx, &store.Outbox{ID: "log-old", AggregateID: "tx", Topic: "servicelogs", Payload: []byte(`{}`), CreatedAt: old})
	_ = mem.MarkOutboxProcessed(ctx, "log-old")
	_ = mem.InsertOutbox(ctx, &store.Outbox{ID: "log-new", AggregateID: "tx", Topic: "servicelogs", Payload: []byte(`{}`), CreatedAt: time.Now()})
	// query returns both, newest first
	rec := httptest.NewRecorder()
	req := httptest.NewRequest("GET", "/v1/logs?limit=10", nil)
	handleLogs(mem)(rec, req)
	if rec.Code != 200 {
		t.Fatalf("logs got %d want 200", rec.Code)
	}
	body := map[string]any{}
	_ = json.NewDecoder(rec.Body).Decode(&body)
	if data, ok := body["data"].([]any); !ok || len(data) != 2 {
		t.Fatalf("want 2 logs got %v", body["data"])
	}
	// retention purges only old processed
	if n, _ := mem.PurgeProcessedLogs(ctx, time.Now().Add(-30*24*time.Hour)); n != 1 {
		t.Fatalf("purged got %d want 1", n)
	}
}

// Phase 2 gate: HMAC-signed callbacks only when NOTIFY_SECRET set.
func TestNotifyHMAC(t *testing.T) {
	t.Setenv("NOTIFY_SECRET", "s3cret")
	t.Setenv("PARTNER_CHARGE_URL", "")
	mem := store.NewMemoryStore()
	svc := lifecycle.New(mem)
	idSvc := identity.NewIdentity(mem, 0, 0, 0, 0, nil, "")
	ctx := context.Background()
	trx, err := svc.CreateOrder(ctx, lifecycle.CreateCmd{Username: "u", UserID: "uid", ProductCode: "P", ProductName: "P", Price: 100, Amount: 1}, "k1")
	if err != nil {
		t.Fatal(err)
	}
	if _, err := svc.Charge(ctx, trx.TransactionID); err != nil {
		t.Fatal(err)
	}
	sign := func(ts, body string) string {
		mac := hmac.New(sha256.New, []byte("s3cret"))
		mac.Write([]byte(ts))
		mac.Write([]byte(body))
		return hex.EncodeToString(mac.Sum(nil))
	}
	body := `{"referenceId":"` + trx.TransactionID + `","status":"SUCCEEDED"}`
	ts := strconv.FormatInt(time.Now().Unix(), 10)
	// valid signature → 202
	rec := httptest.NewRecorder()
	req := httptest.NewRequest("POST", "/ms/api/v1/payment/notify", strings.NewReader(body))
	req.Header.Set("x-callback-timestamp", ts)
	req.Header.Set("x-callback-signature", sign(ts, body))
	handleNotify(svc, idSvc)(rec, req)
	if rec.Code != 202 {
		t.Fatalf("valid sig got %d want 202", rec.Code)
	}
	// forged signature → 401
	rec2 := httptest.NewRecorder()
	req2 := httptest.NewRequest("POST", "/ms/api/v1/payment/notify", strings.NewReader(body))
	req2.Header.Set("x-callback-timestamp", ts)
	req2.Header.Set("x-callback-signature", "forged")
	handleNotify(svc, idSvc)(rec2, req2)
	if rec2.Code != 401 {
		t.Fatalf("forged sig got %d want 401", rec2.Code)
	}
}
