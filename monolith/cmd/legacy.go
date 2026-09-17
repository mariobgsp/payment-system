package main

import (
	"encoding/json"
	"net/http"
	"strings"

	"payment-system/monolith/identity"
	"payment-system/monolith/lifecycle"
	"payment-system/monolith/store"
)

// legacy compat — thin adapters so frontend BFF works unchanged when pointed at monolith.

func handleLegacyLogin(id *identity.Identity) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var body struct{ Username, Password string }
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.Username == "" || body.Password == "" {
			writeJSON(w, 400, map[string]any{"code": "03", "status": "failed", "message": "Invalid value should not be empty"})
			return
		}
		clientIP := firstNonEmpty(strings.Split(r.Header.Get("X-Forwarded-For"), ",")[0], r.RemoteAddr)
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

func productView(p store.Product) map[string]any {
	return map[string]any{"productCode": p.ProductCode, "productName": p.ProductName, "price": p.Price, "discount": p.Discount, "discountAvailable": p.EnableDiscount, "productUpdateDate": "", "productInsertDate": ""}
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
		out := []map[string]any{}
		for _, p := range prods {
			if p.ProductStatus {
				out = append(out, productView(p))
			}
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": out})
	}
}

func handleLegacyUserDetail(s store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		username := firstNonEmpty(r.PathValue("username"), strings.TrimSuffix(strings.TrimPrefix(r.URL.Path, "/ms/api/v1/"), "/user-detail"))
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
		username := firstNonEmpty(r.URL.Query().Get("username"), body.UserDetail.Username)
		if username == "" || body.ProductCode == "" {
			writeJSON(w, 400, map[string]any{"code": "03", "status": "failed", "message": "Invalid value should not be empty"})
			return
		}
		var discount float64
		var userID string
		if prod, _ := s.GetSingleProduct(r.Context(), body.ProductCode); prod != nil {
			discount = prod.Discount
		}
		if u, _ := s.GetUserDetail(r.Context(), username); u != nil {
			userID = u.UserID
		}
		trx, err := svc.CreateOrder(r.Context(), lifecycle.CreateCmd{Username: username, UserID: userID, ProductCode: body.ProductCode, ProductName: body.ProductName, Price: body.Price, Amount: body.Amount, EnableDiscount: body.EnableDiscount, Discount: discount}, r.Header.Get("Idempotency-Key"))
		if err != nil {
			writeErr(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": map[string]any{"transactionId": trx.TransactionID, "createdAt": trx.SysCreationDate}})
	}
}

func legacyTxID(r *http.Request) string {
	parts := strings.Split(r.URL.Path, "/")
	for i, p := range parts {
		if p == "order" && i+1 < len(parts) {
			return parts[i+1]
		}
	}
	return firstNonEmpty(r.PathValue("id"), "")
}

func handleLegacyOrderCheck(svc *lifecycle.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		username := r.URL.Query().Get("username")
		caller := username
		if u, err := svc.Store().GetUserDetail(r.Context(), username); err == nil {
			caller = u.UserID
		}
		trx, err := svc.Check(r.Context(), legacyTxID(r), caller)
		if err != nil {
			writeErr(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": []any{trx}})
	}
}

func handleLegacyPaymentCreate(svc *lifecycle.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		txID := firstNonEmpty(r.URL.Query().Get("transaction_id"), r.URL.Query().Get("transactionId"))
		rs, err := chargeWithPartner(r.Context(), svc, txID, r.PathValue("type"))
		if err != nil {
			// chargeWithPartner returns lifecycle errors for unknown tx, partner errors otherwise
			if _, ok := err.(*lifecycle.ApiError); ok {
				writeErr(w, err)
			} else {
				writeJSON(w, 502, map[string]any{"code": "99", "status": "failed", "message": err.Error()})
			}
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": map[string]any{"CheckoutUrl": rs.CheckoutURL}})
	}
}

func handleLegacyRefund(svc *lifecycle.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var body struct {
			TransactionID  string `json:"transactionId"`
			TransactionID2 string `json:"transaction_id"`
		}
		_ = json.NewDecoder(r.Body).Decode(&body)
		txID := firstNonEmpty(body.TransactionID, body.TransactionID2, r.URL.Query().Get("transaction_id"))
		rs, err := svc.Refund(r.Context(), txID)
		if err != nil {
			writeErr(w, err)
			return
		}
		writeJSON(w, 200, map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": rs})
	}
}
