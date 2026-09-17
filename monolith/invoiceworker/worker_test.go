package main

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"payment-system/monolith/lifecycle"
	"payment-system/monolith/store"
)

// Split-hatch contract: POST raw outbox payload → 200 + receipt file;
// unknown tx → 500 with INV-prefixed error (monolith poller retries → DLQ).
func TestInvoiceEndpoint(t *testing.T) {
	ctx := context.Background()
	mem := store.NewMemoryStore()
	svc := lifecycle.New(mem)
	trx, err := svc.CreateOrder(ctx, lifecycle.CreateCmd{Username: "u", UserID: "uid", ProductCode: "P", ProductName: "P", Price: 100, Amount: 1}, "wk1")
	if err != nil {
		t.Fatal(err)
	}
	if _, err := svc.Charge(ctx, trx.TransactionID); err != nil {
		t.Fatal(err)
	}
	if err := svc.OnCallback(ctx, lifecycle.CallbackEvt{ReferenceID: trx.TransactionID, Status: "SUCCEEDED"}); err != nil {
		t.Fatal(err)
	}
	mux := newMux(mem, t.TempDir())

	rec := httptest.NewRecorder()
	req := httptest.NewRequest("POST", "/invoice", strings.NewReader(`{"transactionId":"`+trx.TransactionID+`","status":"SUCCESS"}`))
	mux.ServeHTTP(rec, req)
	if rec.Code != 200 {
		t.Fatalf("known tx got %d want 200", rec.Code)
	}

	rec2 := httptest.NewRecorder()
	req2 := httptest.NewRequest("POST", "/invoice", strings.NewReader(`{"transactionId":"PTRX-nope","status":"SUCCESS"}`))
	mux.ServeHTTP(rec2, req2)
	if rec2.Code != 500 {
		t.Fatalf("unknown tx got %d want 500", rec2.Code)
	}
	body := map[string]any{}
	_ = json.NewDecoder(rec2.Body).Decode(&body)
	if msg, _ := body["message"].(string); !strings.HasPrefix(msg, "INV:") {
		t.Fatalf("message missing INV prefix: %v", msg)
	}

	rec3 := httptest.NewRecorder()
	mux.ServeHTTP(rec3, httptest.NewRequest("GET", "/health", nil))
	if rec3.Code != http.StatusOK {
		t.Fatalf("health got %d want 200", rec3.Code)
	}
}
