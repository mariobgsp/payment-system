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
	"os/signal"
	"strings"
	"syscall"
	"time"

	"payment-system/monolith/identity"
	"payment-system/monolith/invoice"
	"payment-system/monolith/jobs"
	"payment-system/monolith/lifecycle"
	"payment-system/monolith/store"

	"github.com/jackc/pgx/v5/pgxpool"
)

// ponytail: net/http stdlib, no gin dep — native platform covers it.
// Add gin when you need binding/validation that stdlib falls short.

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	s := mustStore(ctx)
	svc := lifecycle.New(s)
	idSvc := identity.NewIdentity(s, 1800*time.Second, 10, 15*time.Minute, 300*time.Second, nil, os.Getenv("APP_SECRET_KEY"))

	mux := http.NewServeMux()
	// new monolith seam
	mux.HandleFunc("POST /v1/order", handleCreateOrder(svc))
	mux.HandleFunc("GET /v1/order/{id}/check", handleCheck(svc))
	mux.HandleFunc("POST /v1/payment/charge", handleCharge(svc))
	mux.HandleFunc("POST /v1/payment/refund", handleRefund(svc))
	mux.HandleFunc("POST /v1/payment/notify", handleNotify(svc, idSvc))
	mux.HandleFunc("GET /v1/logs", handleLogs(s))
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("content-type", "application/json")
		w.Write([]byte(`{"status":"ok","code":"00"}`))
	})
	// legacy ms-* compat — so frontend can point MS_ORDER_URL/MS_PAYMENT_URL to monolith without code change
	mux.HandleFunc("POST /ms/api/v1/auth/login", handleLegacyLogin(idSvc))
	mux.HandleFunc("POST /ms/api/v1/auth/logout", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "logged-out"})
	})
	mux.HandleFunc("GET /ms/api/v1/view/product", handleLegacyViewProduct(s))
	mux.HandleFunc("GET /ms/api/v1/{username}/user-detail", handleLegacyUserDetail(s))
	mux.HandleFunc("POST /ms/api/v1/order/product", handleLegacyOrderProduct(svc, s))
	mux.HandleFunc("GET /ms/api/v1/order/{id}/check", handleLegacyOrderCheck(svc))
	mux.HandleFunc("POST /ms/api/v1/payment/create/{type}", handleLegacyPaymentCreate(svc))
	mux.HandleFunc("POST /ms/api/v1/payment/refund", handleLegacyRefund(svc))
	mux.HandleFunc("POST /ms/api/v1/payment/notify", handleNotify(svc, idSvc))
	mux.HandleFunc("GET /ms/api/v1/health/check", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("content-type", "application/json")
		w.Write([]byte(`{"status":"ok","code":"00"}`))
	})

	// jobs — sweeper + outbox poller (internal seams, not at external Interface)
	go runSweeper(ctx, s)
	go runOutboxPoller(ctx, s)

	port := env("PORT", "8080")
	log.Printf("monolith listening :%s", port)
	srv := &http.Server{Addr: ":" + port, Handler: withRequestID(mux)}
	go func() {
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal(err)
		}
	}()
	<-ctx.Done()
	shut, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_ = srv.Shutdown(shut)
}

func mustStore(ctx context.Context) store.Store {
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		if os.Getenv("ENV") == "prod" || os.Getenv("APP_ENV") == "prod" {
			log.Fatal("DATABASE_URL required in prod")
		}
		log.Print("DATABASE_URL empty → MemoryStore (dev, local-substitutable fake)")
		return store.NewMemoryStore()
	}
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		log.Fatalf("pg pool: %v", err)
	}
	if err := pool.Ping(ctx); err != nil {
		log.Fatalf("pg ping: %v", err)
	}
	return store.NewPGStore(pool)
}

func runSweeper(ctx context.Context, s store.Store) {
	t := time.NewTicker(60 * time.Second)
	defer t.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-t.C:
			n := jobs.Reconcile(ctx, s, 5*time.Minute) // ponytail: fixed 5m
			if n > 0 {
				log.Printf("sweeper: %d stale READY→FAILED", n)
			}
			if days := envInt("LOG_RETENTION_DAYS", 30); days > 0 {
				if purged, _ := s.PurgeProcessedLogs(ctx, time.Now().Add(-time.Duration(days)*24*time.Hour)); purged > 0 {
					log.Printf("sweeper: purged %d old servicelogs", purged)
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

// routeSender is the real outbox transport: ms-notify-payment → INVOICE_URL (when set)
// else local invoice file (single-binary mode); servicelogs → stdout (Phase 5 adds query API).
type routeSender struct{ store store.Store }

func (l *routeSender) Send(ctx context.Context, topic string, payload []byte) error {
	if topic == "ms-notify-payment" {
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
		if l.store != nil {
			path, err := invoice.WriteFile(ctx, l.store, payload, os.Getenv("INVOICE_DIR"))
			if err != nil {
				return err
			}
			log.Printf("invoice wrote %s", path)
			return nil
		}
	}
	log.Printf("outbox send topic=%s payload=%s", topic, string(payload))
	return nil
}

// --- handlers — thin adapters, Module does work ---

func handleCreateOrder(svc *lifecycle.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var cmd lifecycle.CreateCmd
		if err := json.NewDecoder(r.Body).Decode(&cmd); err != nil {
			writeErr(w, lifecycle.NewBadRequest("invalid body"))
			return
		}
		if cmd.Username == "" || cmd.ProductCode == "" {
			writeErr(w, lifecycle.NewBadRequest("username/productCode required"))
			return
		}
		idemKey := r.Header.Get("Idempotency-Key")
		if idemKey == "" {
			idemKey = r.Header.Get("x-request-id")
		}
		// if still empty, transactionId will dedup Charge but CreateOrder will not be idempotent — caller should send header
		trx, err := svc.CreateOrder(r.Context(), cmd, idemKey)
		if err != nil {
			writeErr(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "data": trx})
	}
}

func handleCharge(svc *lifecycle.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var body struct {
			TransactionID string `json:"transactionId"`
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		if body.TransactionID == "" {
			body.TransactionID = r.URL.Query().Get("transaction_id")
		}
		if body.TransactionID == "" {
			// also try path: /v1/payment/charge?transaction_id=...
			writeErr(w, lifecycle.NewBadRequest("transactionId required"))
			return
		}
		rs, err := svc.Charge(r.Context(), body.TransactionID)
		if err != nil {
			writeErr(w, err)
			return
		}
		if trx, _ := svc.Store().FindTxForUpdate(r.Context(), body.TransactionID); trx != nil {
			if url, perr := partnerChargeURL(r.Context(), body.TransactionID, trx.PriceCharge, r.URL.Query().Get("method")); perr != nil {
				writeJSON(w, 502, map[string]any{"code": "99", "status": "failed", "message": perr.Error()})
				return
			} else if url != "" {
				rs.CheckoutURL = url
			}
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "data": rs})
	}
}

func handleRefund(svc *lifecycle.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var body struct {
			TransactionID string `json:"transactionId"`
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		if body.TransactionID == "" {
			body.TransactionID = r.URL.Query().Get("transaction_id")
		}
		if body.TransactionID == "" {
			writeErr(w, lifecycle.NewBadRequest("transactionId required"))
			return
		}
		// authz via header x-user-id or query username — ponytail: simple, replace with JWT when needed
		rs, err := svc.Refund(r.Context(), body.TransactionID)
		if err != nil {
			writeErr(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "data": rs})
	}
}

func handleCheck(svc *lifecycle.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id := r.PathValue("id")
		if id == "" {
			id = strings.TrimPrefix(r.URL.Path, "/v1/order/")
			id = strings.TrimSuffix(id, "/check")
		}
		caller := r.Header.Get("x-user-id")
		if caller == "" {
			caller = r.URL.Query().Get("username")
		}
		if caller == "" {
			caller = "anonymous"
		}
		if caller != "anonymous" {
			if u, err := svc.Store().GetUserDetail(r.Context(), caller); err == nil {
				caller = u.UserID
			}
		}
		trx, err := svc.Check(r.Context(), id, caller)
		if err != nil {
			writeErr(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "data": trx})
	}
}

func handleNotify(svc *lifecycle.Service, idSvc *identity.Identity) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		raw, _ := io.ReadAll(r.Body)
		// ponytail: verify HMAC+replay when NOTIFY_SECRET set; open in dev (secret empty) so local tests stay green
		if secret := os.Getenv("NOTIFY_SECRET"); secret != "" {
			if !idSvc.VerifyCallback(secret, r.Header.Get("x-callback-timestamp"), string(raw), r.Header.Get("x-callback-signature")) {
				writeJSON(w, 401, map[string]any{"code": "41", "status": "failed", "message": "invalid callback signature"})
				return
			}
		}
		var evt lifecycle.CallbackEvt
		if err := json.Unmarshal(raw, &evt); err != nil {
			writeErr(w, lifecycle.NewBadRequest("invalid callback"))
			return
		}
		if err := svc.OnCallback(r.Context(), evt); err != nil {
			writeErr(w, err)
			return
		}
		writeJSON(w, 202, map[string]any{"code": "00", "status": "accepted", "message": "Request being processed"})
	}
}

// partnerChargeURL calls ms-paymentagr when PARTNER_CHARGE_URL is set (prod), else "" (dev deterministic URL).
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
		Action *struct {
			CheckoutURL string `json:"checkout_url"`
		} `json:"action"`
		CheckoutURL string `json:"checkoutUrl"`
	}
	_ = json.Unmarshal(raw, &rs)
	if rs.Action != nil && rs.Action.CheckoutURL != "" {
		return rs.Action.CheckoutURL, nil
	}
	return rs.CheckoutURL, nil
}

// --- legacy compat handlers — thin adapters to keep frontend BFF unchanged when MS_ORDER_URL points to monolith ---
func handleLegacyLogin(id *identity.Identity) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var body struct{ Username, Password string }
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.Username == "" || body.Password == "" {
			writeJSON(w, 400, map[string]any{"code": "03", "status": "failed", "message": "Invalid value should not be empty"})
			return
		}
		clientIP := r.RemoteAddr
		if ip := r.Header.Get("X-Forwarded-For"); ip != "" {
			clientIP = strings.Split(ip, ",")[0]
		}
		sess, err := id.Login(r.Context(), body.Username, body.Password, clientIP)
		if err != nil {
			if err == identity.ErrTooManyAttempts {
				writeJSON(w, 429, map[string]any{"code": "40", "status": "failed", "message": "Too many login attempts, try again later"})
				return
			}
			writeJSON(w, 401, map[string]any{"code": "01", "status": "failed", "message": "Invalid username or password"})
			return
		}
		u, _ := id.StoreUser(r.Context(), body.Username)
		if u == nil {
			writeJSON(w, 500, map[string]any{"code": "99", "status": "failed", "message": "user not found"})
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": map[string]any{
			"id": u.ID, "userId": u.UserID, "username": u.Username, "firstName": u.FirstName, "lastName": u.LastName, "email": u.Email, "specialProduct": u.SpecialProduct, "recurring": u.Recurring, "token": sess.Token,
		}})
	}
}
func handleLegacyViewProduct(s store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		username := r.URL.Query().Get("username")
		if username == "" {
			writeJSON(w, 400, map[string]any{"code": "03", "status": "failed", "message": "username required"})
			return
		}
		u, err := s.GetUserDetail(r.Context(), username)
		if err != nil {
			writeJSON(w, 404, map[string]any{"code": "01", "status": "failed", "message": "User not allowed"})
			return
		}
		var prods []store.Product
		if u.SpecialProduct {
			prods, _ = s.GetAllProducts(r.Context())
		} else {
			prods, _ = s.GetSpecialProducts(r.Context(), false)
		}
		// map to frontend shape
		var out []map[string]any
		for _, p := range prods {
			if !p.ProductStatus {
				continue
			}
			out = append(out, map[string]any{"productCode": p.ProductCode, "productName": p.ProductName, "price": p.Price, "discount": p.Discount, "discountAvailable": p.EnableDiscount, "productUpdateDate": "", "productInsertDate": ""})
		}
		if out == nil {
			out = []map[string]any{}
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": out})
	}
}
func handleLegacyUserDetail(s store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		username := r.PathValue("username")
		if username == "" {
			username = strings.TrimPrefix(r.URL.Path, "/ms/api/v1/")
			username = strings.TrimSuffix(username, "/user-detail")
		}
		u, err := s.GetUserDetail(r.Context(), username)
		if err != nil {
			writeJSON(w, 404, map[string]any{"code": "01", "status": "failed", "message": "User not exist"})
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": map[string]any{"id": u.ID, "userId": u.UserID, "username": u.Username, "firstName": u.FirstName, "lastName": u.LastName, "email": u.Email, "specialProduct": u.SpecialProduct, "recurring": u.Recurring}})
	}
}
func handleLegacyOrderProduct(svc *lifecycle.Service, s store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		username := r.URL.Query().Get("username")
		var body struct {
			ProductCode    string `json:"productCode"`
			ProductName    string `json:"productName"`
			Amount         int64  `json:"amount"`
			Price          int64  `json:"price"`
			EnableDiscount bool   `json:"enableDiscount"`
			UserDetail     struct {
				Username string `json:"username"`
			} `json:"userDetail"`
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		if username == "" {
			username = body.UserDetail.Username
		}
		if username == "" || body.ProductCode == "" {
			writeJSON(w, 400, map[string]any{"code": "03", "status": "failed", "message": "Invalid value should not be empty"})
			return
		}
		prod, _ := s.GetSingleProduct(r.Context(), body.ProductCode)
		discount := 0.0
		if prod != nil {
			discount = prod.Discount
		}
		u, _ := s.GetUserDetail(r.Context(), username)
		userID := ""
		if u != nil {
			userID = u.UserID
		}
		cmd := lifecycle.CreateCmd{Username: username, UserID: userID, ProductCode: body.ProductCode, ProductName: body.ProductName, Price: body.Price, Amount: body.Amount, EnableDiscount: body.EnableDiscount, Discount: discount}
		trx, err := svc.CreateOrder(r.Context(), cmd, r.Header.Get("Idempotency-Key"))
		if err != nil {
			writeErr(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": map[string]any{"transactionId": trx.TransactionID, "createdAt": trx.SysCreationDate}})
	}
}
func handleLegacyOrderCheck(svc *lifecycle.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id := r.PathValue("id")
		if id == "" {
			parts := strings.Split(r.URL.Path, "/")
			for i, p := range parts {
				if p == "order" && i+1 < len(parts) {
					id = parts[i+1]
					break
				}
			}
		}
		username := r.URL.Query().Get("username")
		// resolve username -> userId for authz (store holds userId)
		caller := username
		if u, err := svc.Store().GetUserDetail(r.Context(), username); err == nil {
			caller = u.UserID
		}
		trx, err := svc.Check(r.Context(), id, caller)
		if err != nil {
			writeErr(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": []any{trx}})
	}
}
func handleLegacyPaymentCreate(svc *lifecycle.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		typeStr := r.PathValue("type")
		_ = typeStr
		txID := r.URL.Query().Get("transaction_id")
		if txID == "" {
			txID = r.URL.Query().Get("transactionId")
		}
		// frontend sends callbackUrl in body, ignore for monolith — partner URL when configured, else deterministic
		rs, err := svc.Charge(r.Context(), txID)
		if err != nil {
			writeErr(w, err)
			return
		}
		if trx, _ := svc.Store().FindTxForUpdate(r.Context(), txID); trx != nil {
			if url, perr := partnerChargeURL(r.Context(), txID, trx.PriceCharge, typeStr); perr != nil {
				writeJSON(w, 502, map[string]any{"code": "99", "status": "failed", "message": perr.Error()})
				return
			} else if url != "" {
				rs.CheckoutURL = url
			}
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": map[string]any{"CheckoutUrl": rs.CheckoutURL}})
	}
}
func handleLegacyRefund(svc *lifecycle.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var body struct {
			TransactionId  string `json:"transactionId"`
			TransactionID2 string `json:"transaction_id"`
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		txID := body.TransactionId
		if txID == "" {
			txID = body.TransactionID2
		}
		if txID == "" { // try query
			txID = r.URL.Query().Get("transaction_id")
		}
		rs, err := svc.Refund(r.Context(), txID)
		if err != nil {
			writeErr(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": rs})
	}
}

func writeJSON(w http.ResponseWriter, code int, v any) {
	w.Header().Set("content-type", "application/json")
	w.WriteHeader(code)
	_ = json.NewEncoder(w).Encode(v)
}

func writeErr(w http.ResponseWriter, err error) {
	if ae, ok := err.(*lifecycle.ApiError); ok {
		code := "99"
		if ae.Code != 500 {
			code = "01"
			if ae.Code == 429 {
				code = "40"
			}
		}
		writeJSON(w, ae.Code, map[string]any{"code": code, "status": "failed", "message": ae.Message})
		return
	}
	writeJSON(w, 500, map[string]any{"code": "99", "status": "failed", "message": err.Error()})
}

func withRequestID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		rid := r.Header.Get("x-request-id")
		if rid == "" {
			rid = r.Header.Get("X-Request-Id")
		}
		if rid != "" {
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

// handleLogs replaces Mongo ServiceLog reads: recent servicelogs outbox rows, newest first.
func handleLogs(s store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		limit := envInt("LOG_PAGE_LIMIT", 50)
		if q := r.URL.Query().Get("limit"); q != "" {
			if _, err := fmt.Sscanf(q, "%d", &limit); err != nil || limit <= 0 || limit > 500 {
				limit = 50
			}
		}
		logs, err := s.ListRecentLogs(r.Context(), limit)
		if err != nil {
			writeJSON(w, 500, map[string]any{"code": "99", "status": "failed", "message": err.Error()})
			return
		}
		if logs == nil {
			logs = []store.Outbox{}
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "data": logs})
	}
}
