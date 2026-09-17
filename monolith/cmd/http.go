package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"time"

	"payment-system/monolith/invoice"
	"payment-system/monolith/jobs"
	"payment-system/monolith/lifecycle"
	"payment-system/monolith/store"
)

// firstNonEmpty returns the first non-empty value — replaces if-if-if fallback chains.
func firstNonEmpty(vals ...string) string {
	for _, v := range vals {
		if v != "" {
			return v
		}
	}
	return ""
}

func writeJSON(w http.ResponseWriter, code int, v any) {
	w.Header().Set("content-type", "application/json")
	w.WriteHeader(code)
	_ = json.NewEncoder(w).Encode(v)
}

// errCode maps lifecycle ApiError HTTP status → envelope code.
func errCode(httpStatus int) string {
	switch httpStatus {
	case 429:
		return "40"
	case 500:
		return "99"
	default:
		return "01"
	}
}

func writeErr(w http.ResponseWriter, err error) {
	if ae, ok := err.(*lifecycle.ApiError); ok {
		writeJSON(w, ae.Code, map[string]any{"code": errCode(ae.Code), "status": "failed", "message": ae.Message})
		return
	}
	writeJSON(w, 500, map[string]any{"code": "99", "status": "failed", "message": err.Error()})
}

func ok(w http.ResponseWriter, data any) {
	writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "data": data})
}

func withRequestID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Header.Get is case-insensitive — single lookup covers all casings.
		if rid := r.Header.Get("x-request-id"); rid != "" {
			w.Header().Set("x-request-id", rid)
		}
		next.ServeHTTP(w, r)
	})
}

func env(k, def string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return def
}

func envInt(k string, def int) int {
	if v := os.Getenv(k); v != "" {
		var n int
		if _, err := fmt.Sscanf(v, "%d", &n); err == nil {
			return n
		}
	}
	return def
}

func atoiOr(s string, def int) int {
	if s == "" {
		return def
	}
	var n int
	if _, err := fmt.Sscanf(s, "%d", &n); err == nil {
		return n
	}
	return def
}

// partnerChargeURL calls ms-paymentagr when PARTNER_CHARGE_URL is set, else "".
// Fail-closed: partner error → caller surfaces 502, never silent fallback.
func partnerChargeURL(ctx context.Context, txID string, amount int64, method string) (string, error) {
	base := os.Getenv("PARTNER_CHARGE_URL")
	if base == "" {
		return "", nil
	}
	if method == "" {
		method = "SHOPEEPAY"
	}
	body, _ := json.Marshal(map[string]any{"reference_id": txID, "currency": "IDR", "checkout_method": method, "amount": amount, "payment_code": method, "redirect_url": os.Getenv("FRONTEND_URL"), "callback_url": os.Getenv("MONOLITH_PUBLIC_URL")})
	req, _ := http.NewRequestWithContext(ctx, "POST", base, bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	if k := os.Getenv("PARTNER_API_KEY"); k != "" {
		req.Header.Set("api-key", k)
	}
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("partner charge %d: %s", resp.StatusCode, string(raw))
	}
	var rs struct {
		Action      *struct{ CheckoutURL string `json:"checkout_url"` } `json:"action"`
		CheckoutURL string `json:"checkoutUrl"`
	}
	_ = json.Unmarshal(raw, &rs)
	if rs.Action != nil {
		return rs.Action.CheckoutURL, nil
	}
	return rs.CheckoutURL, nil
}

// chargeWithPartner runs lifecycle Charge then attaches partner checkout URL.
// Shared by new + legacy payment-create handlers (was duplicated).
func chargeWithPartner(ctx context.Context, svc *lifecycle.Service, txID, method string) (*lifecycle.ChargeRs, error) {
	rs, err := svc.Charge(ctx, txID)
	if err != nil {
		return nil, err
	}
	trx, _ := svc.Store().FindTxForUpdate(ctx, txID)
	if trx == nil {
		return rs, nil
	}
	url, perr := partnerChargeURL(ctx, txID, trx.PriceCharge, method)
	if perr != nil {
		return nil, perr
	}
	if url != "" {
		rs.CheckoutURL = url
	}
	return rs, nil
}

// dlqOutbox reuses the outbox table (topic=dlq.<orig>) — no schema change.
// ponytail: split to dedicated table if DLQ volume matters.
type dlqOutbox struct{ store store.Store }

func (d *dlqOutbox) InsertDLQ(ctx context.Context, ob store.Outbox, lastErr string) error {
	return d.store.InsertOutbox(ctx, &store.Outbox{
		ID:          ob.ID + "-dlq",
		AggregateID: ob.AggregateID,
		Topic:       "dlq." + ob.Topic,
		Payload:     []byte(`{"orig":"` + ob.ID + `","err":"` + lastErr + `"}`),
		CreatedAt:   time.Now(),
	})
}

// routeSender is the outbox transport: ms-notify-payment → INVOICE_URL or local file.
type routeSender struct{ store store.Store }

func (l *routeSender) Send(ctx context.Context, topic string, payload []byte) error {
	if topic != "ms-notify-payment" {
		log.Printf("JOB: outbox send topic=%s payload=%s", topic, string(payload))
		return nil
	}
	if url := os.Getenv("INVOICE_URL"); url != "" {
		req, _ := http.NewRequestWithContext(ctx, "POST", url, bytes.NewReader(payload))
		req.Header.Set("Content-Type", "application/json")
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			return err
		}
		defer resp.Body.Close()
		if resp.StatusCode < 200 || resp.StatusCode >= 300 {
			raw, _ := io.ReadAll(resp.Body)
			return fmt.Errorf("invoice %d: %s", resp.StatusCode, string(raw))
		}
		return nil
	}
	if l.store == nil {
		return nil
	}
	path, err := invoice.WriteFile(ctx, l.store, payload, os.Getenv("INVOICE_DIR"))
	if err != nil {
		return err
	}
	log.Printf("INV: invoice wrote %s", path)
	return nil
}

func runSweeper(ctx context.Context, s store.Store) {
	t := time.NewTicker(60 * time.Second)
	defer t.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-t.C:
			if n := jobs.Reconcile(ctx, s, 5*time.Minute); n > 0 { // ponytail: fixed 5m
				log.Printf("JOB: sweeper %d stale READY→FAILED", n)
			}
			if days := envInt("LOG_RETENTION_DAYS", 30); days > 0 {
				if purged, _ := s.PurgeProcessedLogs(ctx, time.Now().Add(-time.Duration(days)*24*time.Hour)); purged > 0 {
					log.Printf("JOB: sweeper purged %d old servicelogs", purged)
				}
			}
		}
	}
}

func runOutboxPoller(ctx context.Context, s store.Store) {
	sender := &routeSender{store: s}
	poller := &jobs.Poller{Store: s, Send: sender, DLQStore: &dlqOutbox{store: s}}
	poller.Loop(ctx, 5*time.Second)
}
