package main

import (
	"net/http"

	"payment-system/monolith/identity"
	"payment-system/monolith/lifecycle"
	"payment-system/monolith/store"
)

type route struct {
	method, pattern string
	handler         http.HandlerFunc
}

// routes replaces 15 sequential mux.HandleFunc calls with a table — one place to see all endpoints.
func routes(svc *lifecycle.Service, idSvc *identity.Identity, s store.Store) []route {
	health := func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("content-type", "application/json")
		w.Write([]byte(`{"status":"ok","code":"00"}`))
	}
	logout := func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "logged-out"})
	}
	return []route{
		{"POST", "/v1/order", handleCreateOrder(svc)},
		{"GET", "/v1/order/{id}/check", handleCheck(svc)},
		{"POST", "/v1/payment/charge", handleCharge(svc)},
		{"POST", "/v1/payment/refund", handleRefund(svc)},
		{"POST", "/v1/payment/notify", handleNotify(svc, idSvc)},
		{"GET", "/v1/logs", handleLogs(s)},
		{"GET", "/health", health},
		// legacy compat — frontend can point MS_ORDER_URL/MS_PAYMENT_URL at monolith
		{"POST", "/ms/api/v1/auth/login", handleLegacyLogin(idSvc)},
		{"POST", "/ms/api/v1/auth/logout", logout},
		{"GET", "/ms/api/v1/view/product", handleLegacyViewProduct(s)},
		{"GET", "/ms/api/v1/{username}/user-detail", handleLegacyUserDetail(s)},
		{"POST", "/ms/api/v1/order/product", handleLegacyOrderProduct(svc, s)},
		{"GET", "/ms/api/v1/order/{id}/check", handleLegacyOrderCheck(svc)},
		{"POST", "/ms/api/v1/payment/create/{type}", handleLegacyPaymentCreate(svc)},
		{"POST", "/ms/api/v1/payment/refund", handleLegacyRefund(svc)},
		{"POST", "/ms/api/v1/payment/notify", handleNotify(svc, idSvc)},
		{"GET", "/ms/api/v1/health/check", health},
	}
}

func newMux(svc *lifecycle.Service, idSvc *identity.Identity, s store.Store) *http.ServeMux {
	mux := http.NewServeMux()
	for _, r := range routes(svc, idSvc, s) {
		mux.HandleFunc(r.method+" "+r.pattern, r.handler)
	}
	return mux
}
