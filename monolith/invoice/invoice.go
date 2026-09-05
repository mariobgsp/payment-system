package invoice

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"time"

	"payment-system/monolith/store"
)

var safeTx = regexp.MustCompile(`[^a-zA-Z0-9\-_]`)

// WriteFile is the Go replacement for JasperReports PDFs: same fields
// (transactionId, product, price, paymentDate) as JSON receipt.
// ponytail: JSON receipt, not byte-identical PDF — upgrade to PDF lib if finance requires it.
func WriteFile(ctx context.Context, s store.Store, payload []byte, dir string) (string, error) {
	var evt struct {
		TransactionID string `json:"transactionId"`
	}
	if err := json.Unmarshal(payload, &evt); err != nil || evt.TransactionID == "" {
		return "", fmt.Errorf("invalid invoice payload")
	}
	trx, err := s.FindTxForUpdate(ctx, evt.TransactionID)
	if err != nil || trx == nil {
		return "", fmt.Errorf("transaction not found: %s", evt.TransactionID)
	}
	safe := safeTx.ReplaceAllString(evt.TransactionID, "")
	if dir == "" {
		dir = "invoices"
	}
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return "", err
	}
	now := time.Now()
	doc := map[string]any{
		"transactionId": trx.TransactionID,
		"productName":   trx.ProductName,
		"productCode":   trx.ProductCode,
		"price":         trx.Price,
		"priceCharge":   trx.PriceCharge,
		"paymentStatus": trx.PaymentStatus,
		"orderStatus":   trx.OrderStatus,
		"userId":        trx.UserID,
		"generatedAt":   now.Format(time.RFC3339),
	}
	if trx.PaymentDate != nil {
		doc["paymentDate"] = trx.PaymentDate.Format("2006-01-02 15:04:05")
	}
	b, _ := json.MarshalIndent(doc, "", "  ")
	path := filepath.Join(dir, "invoice_"+safe+".json")
	if err := os.WriteFile(path, b, 0o644); err != nil {
		return "", err
	}
	return path, nil
}
