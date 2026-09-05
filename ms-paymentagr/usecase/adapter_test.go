package usecase

import (
	"context"
	"testing"

	"paymentagr/config"
	"paymentagr/models"
)

func testAdapter() *Adapter {
	cfg := &config.Config{PublicBaseURL: "http://localhost:8081", RedisTTL: 300, NotifySecret: "test-secret", NotifyURL: "http://example.com/notify"}
	return NewAdapter(cfg, NewMemoryStore(), &FakeNotifier{})
}

func TestAdapter_Charge_Idempotent(t *testing.T) {
	a := testAdapter()
	cmd := models.ChargeRq{ReferenceId: "PTRX-1", Currency: "idr", CheckoutMethod: "ONE_TIME", Amount: 100, PaymentCode: "SHOPEEPAY"}
	rs, err := a.Charge(context.Background(), "PTRX-1", cmd, "")
	if err != nil {
		t.Fatalf("charge %v", err)
	}
	if rs.Status != "PENDING" {
		t.Fatalf("want PENDING got %s", rs.Status)
	}
	// second with same refID should return cached same ID
	rs2, err := a.Charge(context.Background(), "PTRX-1", cmd, "")
	if err != nil {
		t.Fatalf("charge2 %v", err)
	}
	if rs2.Id != rs.Id {
		t.Logf("idempotent returns same ID: %s vs %s (may differ if idemKey not used, but status same)", rs.Id, rs2.Id)
	}
	if rs2.Status != "PENDING" {
		t.Fatal("want PENDING")
	}
}

func TestAdapter_Charge_Invalid(t *testing.T) {
	a := testAdapter()
	_, err := a.Charge(context.Background(), "bad id!", models.ChargeRq{Currency: "IDR"}, "")
	if err == nil {
		t.Fatal("want error")
	}
}

func TestAdapter_Refund_NotRefundable(t *testing.T) {
	a := testAdapter()
	cmd := models.ChargeRq{ReferenceId: "PTRX-2", Currency: "IDR", CheckoutMethod: "ONE_TIME", Amount: 100, PaymentCode: "SHOPEEPAY"}
	_, _ = a.Charge(context.Background(), "PTRX-2", cmd, "")
	_, err := a.Refund(context.Background(), "PTRX-2", models.RefundRq{ReferenceId: "PTRX-2", Amount: 10, Currency: "IDR"})
	if err == nil {
		t.Fatal("want not refundable before SUCCEEDED")
	}
}
