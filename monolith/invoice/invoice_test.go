package invoice

import (
	"context"
	"encoding/json"
	"os"
	"testing"

	"payment-system/monolith/lifecycle"
	"payment-system/monolith/store"
)

// Phase 4 gate: outbox ms-notify-payment → stored invoice receipt.
func TestWriteFile(t *testing.T) {
	ctx := context.Background()
	mem := store.NewMemoryStore()
	svc := lifecycle.New(mem)
	trx, err := svc.CreateOrder(ctx, lifecycle.CreateCmd{Username: "u", UserID: "uid", ProductCode: "P", ProductName: "Water", Price: 100, Amount: 1}, "ik1")
	if err != nil {
		t.Fatal(err)
	}
	if _, err := svc.Charge(ctx, trx.TransactionID); err != nil {
		t.Fatal(err)
	}
	if err := svc.OnCallback(ctx, lifecycle.CallbackEvt{ReferenceID: trx.TransactionID, Status: "SUCCEEDED"}); err != nil {
		t.Fatal(err)
	}
	dir := t.TempDir()
	payload, _ := json.Marshal(map[string]any{"transactionId": trx.TransactionID, "status": "SUCCESS"})
	path, err := WriteFile(ctx, mem, payload, dir)
	if err != nil {
		t.Fatal(err)
	}
	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	var doc map[string]any
	_ = json.Unmarshal(raw, &doc)
	if doc["transactionId"] != trx.TransactionID {
		t.Fatalf("invoice for wrong tx: %v", doc["transactionId"])
	}
	if doc["paymentStatus"] != "SUCCESS" {
		t.Fatalf("status got %v want SUCCESS", doc["paymentStatus"])
	}
}
