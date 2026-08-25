package lifecycle

import (
	"context"
	"testing"
	"time"

	"payment-system/monolith/store"
)

// Interface is test surface — tests survive internal refactors, assert observable outcomes.

func TestCreateOrder_PricingAndIdempotency(t *testing.T) {
	mem := store.NewMemoryStore()
	svc := New(mem)
	ctx := context.Background()
	cmd := CreateCmd{Username: "klhomme0", UserID: "b2xrasd", ProductCode: "TJX-99896", ProductName: "Carbonated Water", Price: 61557, Amount: 2, EnableDiscount: false, Discount: 0.71}
	trx, err := svc.CreateOrder(ctx, cmd, "idem-1")
	if err != nil {
		t.Fatalf("create: %v", err)
	}
	if trx.PriceCharge != 123114 {
		t.Fatalf("priceCharge got %d want 123114 (no discount)", trx.PriceCharge)
	}
	// discount enabled
	cmd2 := CreateCmd{Username: "klhomme0", UserID: "b2xrasd", ProductCode: "IDM-44572", ProductName: "Radish", Price: 11362, Amount: 2, EnableDiscount: true, Discount: 0.3}
	trx2, err := svc.CreateOrder(ctx, cmd2, "idem-2")
	if err != nil {
		t.Fatalf("create discount: %v", err)
	}
	want2, _ := calcPriceCharge(11362, 0.3, 2, true)
	if trx2.PriceCharge != want2 {
		t.Fatalf("got %d want %d", trx2.PriceCharge, want2)
	}
	// idempotency: second CreateOrder with same idemKey returns same transaction (cached)
	trxDup, err := svc.CreateOrder(ctx, cmd, "idem-1")
	if err != nil {
		t.Fatalf("idem duplicate: %v", err)
	}
	if trxDup.TransactionID != trx.TransactionID {
		t.Fatalf("idem failed: got %s want %s", trxDup.TransactionID, trx.TransactionID)
	}
	// enable requested but discount 0 → 400
	cmdBad := CreateCmd{Username: "klhomme0", UserID: "b2xrasd", ProductCode: "TVW-44725", ProductName: "Cheese", Price: 19959, Amount: 1, EnableDiscount: true, Discount: 0}
	if _, err := svc.CreateOrder(ctx, cmdBad, "idem-bad"); err == nil {
		t.Fatal("want error when discount not available but enable true")
	}
}

func TestCharge_TransitionAndIdempotent(t *testing.T) {
	mem := store.NewMemoryStore()
	svc := New(mem)
	ctx := context.Background()
	cmd := CreateCmd{Username: "ewhicher1", UserID: "rebajea", ProductCode: "BUM-24071", ProductName: "Wine", Price: 45647, Amount: 1, EnableDiscount: false}
	trx, _ := svc.CreateOrder(ctx, cmd, "c1")
	// CREATED→READY
	rs, err := svc.Charge(ctx, trx.TransactionID)
	if err != nil {
		t.Fatalf("charge: %v", err)
	}
	if rs.Status != "READY" {
		t.Fatalf("want READY got %s", rs.Status)
	}
	// idempotent second charge returns same
	rs2, err := svc.Charge(ctx, trx.TransactionID)
	if err != nil {
		t.Fatalf("charge2: %v", err)
	}
	if rs2.TransactionID != rs.TransactionID {
		t.Fatalf("idempotent mismatch")
	}
	// third charge again still READY
	rs3, _ := svc.Charge(ctx, trx.TransactionID)
	if rs3.Status != "READY" {
		t.Fatalf("want READY on repeat")
	}
}

func TestRefund_InvalidTransition(t *testing.T) {
	mem := store.NewMemoryStore()
	svc := New(mem)
	ctx := context.Background()
	cmd := CreateCmd{Username: "admin1", UserID: "v3jjsv0", ProductCode: "GDL-75914", ProductName: "Pepper", Price: 86744, Amount: 1, EnableDiscount: false}
	trx, _ := svc.CreateOrder(ctx, cmd, "r1")
	// REFUND on CREATED → 409
	if _, err := svc.Refund(ctx, trx.TransactionID); err == nil {
		t.Fatal("want 409 when refund CREATED")
	} else if ae, ok := err.(*ApiError); !ok || ae.Code != 409 {
		t.Fatalf("want 409 got %v", err)
	}
	// CREATED→READY→SUCCESS then refund ok
	_, _ = svc.Charge(ctx, trx.TransactionID)
	if err := svc.OnCallback(ctx, CallbackEvt{ReferenceID: trx.TransactionID, Status: "SUCCEEDED"}); err != nil {
		t.Fatalf("callback: %v", err)
	}
	rs, err := svc.Refund(ctx, trx.TransactionID)
	if err != nil {
		t.Fatalf("refund after success: %v", err)
	}
	if rs.Status != "REFUND" {
		t.Fatalf("want REFUND got %s", rs.Status)
	}
	// second refund → 409
	if _, err := svc.Refund(ctx, trx.TransactionID); err == nil {
		t.Fatal("want 409 on second refund")
	}
}

func TestCheck_Authz(t *testing.T) {
	mem := store.NewMemoryStore()
	svc := New(mem)
	ctx := context.Background()
	cmd := CreateCmd{Username: "klhomme0", UserID: "b2xrasd", ProductCode: "TJX-99896", ProductName: "Water", Price: 61557, Amount: 1, EnableDiscount: false}
	trx, _ := svc.CreateOrder(ctx, cmd, "a1")
	// correct owner → ok
	if _, err := svc.Check(ctx, trx.TransactionID, "b2xrasd"); err != nil {
		t.Fatalf("check owner: %v", err)
	}
	// wrong owner → 401
	if _, err := svc.Check(ctx, trx.TransactionID, "rebajea"); err == nil {
		t.Fatal("want 401 wrong owner")
	} else if ae, ok := err.(*ApiError); !ok || ae.Code != 401 {
		t.Fatalf("want 401 got %v", err)
	}
}

func TestOnCallback_IdempotentAndFailed(t *testing.T) {
	mem := store.NewMemoryStore()
	svc := New(mem)
	ctx := context.Background()
	cmd := CreateCmd{Username: "jdecreuze2", UserID: "7qr8lsq", ProductCode: "NZH-97257", ProductName: "Isomalt", Price: 95923, Amount: 1, EnableDiscount: false}
	trx, _ := svc.CreateOrder(ctx, cmd, "cb1")
	_, _ = svc.Charge(ctx, trx.TransactionID)
	// first SUCCEEDED → SUCCESS
	if err := svc.OnCallback(ctx, CallbackEvt{ReferenceID: trx.TransactionID, Status: "SUCCEEDED"}); err != nil {
		t.Fatalf("callback: %v", err)
	}
	// second same callback → idempotent nil
	if err := svc.OnCallback(ctx, CallbackEvt{ReferenceID: trx.TransactionID, Status: "SUCCEEDED"}); err != nil {
		t.Fatalf("idempotent callback: %v", err)
	}
	// FAILED path on fresh trx
	trx2, _ := svc.CreateOrder(ctx, CreateCmd{Username: "admin1", UserID: "v3jjsv0", ProductCode: "PAD-05945", ProductName: "Kiwi", Price: 82270, Amount: 1}, "cb2")
	_, _ = svc.Charge(ctx, trx2.TransactionID)
	if err := svc.OnCallback(ctx, CallbackEvt{ReferenceID: trx2.TransactionID, Status: "FAILED"}); err != nil {
		t.Fatalf("failed callback: %v", err)
	}
	// after FAILED, refund → 409
	if _, err := svc.Refund(ctx, trx2.TransactionID); err == nil {
		t.Fatal("want 409 refund after FAILED")
	}
}

func TestPricing_Edge(t *testing.T) {
	if _, err := calcPriceCharge(0, 0.3, 1, true); err == nil {
		t.Fatal("want error price 0")
	}
	if _, err := calcPriceCharge(100, 0.3, 0, true); err == nil {
		t.Fatal("want error amount 0")
	}
	// discount disabled → full price
	v, _ := calcPriceCharge(10000, 0.5, 2, false)
	if v != 20000 {
		t.Fatalf("want 20000 got %d", v)
	}
}

func TestReconcile_Sweeper(t *testing.T) {
	mem := store.NewMemoryStore()
	svc := New(mem)
	ctx := context.Background()
	// create old READY row manually via store
	trx := store.ProductTrx{ID: "MSO-old", TransactionID: "PTRX-OLD", OrderStatus: "READY", PaymentStatus: "READY", UserID: "b2xrasd", PriceCharge: 100, Amount: 1, Price: 100, ProductCode: "TJX-99896", Discount: 0}
	oldTime := time.Now().Add(-10 * time.Minute)
	trx.SysCreationDate = &oldTime
	_ = mem.InsertTxWithOutbox(ctx, trx, nil, "")
	_, _ = svc.Charge(ctx, "PTRX-OLD") // ensures READY already; insert already READY so skip
	// use jobs.Reconcile via store directly? test via store.FindStaleReady
	stale, _ := mem.FindStaleReady(ctx, time.Now().Add(-5*time.Minute), 10)
	if len(stale) == 0 {
		t.Fatal("want stale found")
	}
	// simulate Reconcile effect: update
	for _, s := range stale {
		_ = mem.UpdateStatusWithOutbox(ctx, s.TransactionID, s.OrderStatus, "FAILED", nil)
	}
	after, _ := mem.FindTxForUpdate(ctx, "PTRX-OLD")
	if after.PaymentStatus != "FAILED" {
		t.Fatalf("want FAILED got %s", after.PaymentStatus)
	}
}
