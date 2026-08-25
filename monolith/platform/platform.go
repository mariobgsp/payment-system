package platform

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"sync"
	"time"

	"payment-system/monolith/lifecycle"
	"payment-system/monolith/store"

	"github.com/google/uuid"
)

// Platform is the deep Module that hides request construction, response envelopes,
// header propagation, ServiceLog building, and date formatting.
//
// External Interface (5 entry points):
//   - Request(channel, opName, requestId, payload, r) RequestInfo
//   - Ok(req, body) ResponseInfo
//   - Accepted(req) ResponseInfo
//   - Fail(req, err) ResponseInfo
//   - Publish(ctx, req, resp) error  (outbox insert via store.Store)
//
// Internal seam: Clock (for requestAt / ServiceLog timestamps).
// No adapters at external Interface; Store is Local-substitutable but only for Publish.
type Platform struct {
	Store Store
	Now   func() time.Time // test clock injection; nil => time.Now
	AppName string         // e.g. "monolith"
}

type Store = store.Store

// RequestInfo mirrors Java RequestInfo but minimal and hidden behind Platform.
type RequestInfo struct {
	AppName       string
	Channel       string
	OpName        string
	RequestID     string
	CorrelationID string
	RequestAt     string // Asia/Jakarta formatted, thread-safe via time.Format
	Payload       any
	Method        string
	URI           string
	Host          string
	Query         string
}

// ResponseInfo mirrors Java ResponseInfo.
type ResponseInfo struct {
	Status  int
	Headers map[string]string
	Body    any // envelope {code,status,message,data}
}

func (p *Platform) now() time.Time {
	if p.Now != nil {
		return p.Now()
	}
	return time.Now()
}

func (p *Platform) appName() string {
	if p.AppName != "" {
		return p.AppName
	}
	return "monolith"
}

var jakartaLoc = time.FixedZone("WIB", 7*3600)
var jakartaOnce sync.Once

func jakartaTime(t time.Time) string {
	jakartaOnce.Do(func() {
		if loc, err := time.LoadLocation("Asia/Jakarta"); err == nil {
			jakartaLoc = loc
		}
	})
	return t.In(jakartaLoc).Format("2006-01-02 15:04:05")
}

// RequestOpts bundles F1 Too Many Args fix — single struct instead of 5 args.
type RequestOpts struct {
	Channel, OpName, RequestID string
	Payload any
	R       *http.Request
}
func (p *Platform) Request(opts RequestOpts) RequestInfo {
	channel := p.resolveChannel(opts.Channel, opts.R)
	reqID := p.resolveRequestID(opts.RequestID, opts.R)
	now := p.now()
	info := RequestInfo{
		AppName:       p.appName(),
		Channel:       channel,
		OpName:        opts.OpName,
		RequestID:     reqID,
		CorrelationID: "monolith-" + uuid.NewString(),
		RequestAt:     jakartaTime(now),
		Payload:       opts.Payload,
	}
	p.fillHTTP(&info, opts.R)
	return info
}
func (p *Platform) resolveChannel(channel string, r *http.Request) string {
	if channel != "" { return channel }
	if r != nil {
		if v := r.Header.Get("x-request-channel"); v != "" { return v }
		if v := r.Header.Get("X-Request-Channel"); v != "" { return v }
	}
	return "WEB"
}
func (p *Platform) resolveRequestID(id string, r *http.Request) string {
	if id != "" { return id }
	if r != nil {
		if v := r.Header.Get("x-request-id"); v != "" { return v }
		if v := r.Header.Get("X-Request-Id"); v != "" { return v }
	}
	return uuid.NewString()
}
func (p *Platform) fillHTTP(info *RequestInfo, r *http.Request) {
	if r == nil || r.URL == nil { // G34: Stepdown — guard nil URL (test Request with no URL)
		if r != nil {
			info.Method = r.Method
			info.Host = r.RemoteAddr
		}
		return
	}
	info.Method = r.Method
	info.Host = r.RemoteAddr
	info.Query = r.URL.RawQuery
	info.URI = r.URL.Path
	if r.URL.RawQuery != "" {
		info.URI += "?" + r.URL.RawQuery
	}
}

// headerMap builds x-request-* headers hidden from callers.
func headerMap(req RequestInfo) map[string]string {
	return map[string]string{
		"x-request-id": req.RequestID,
		"x-channel-id": req.Channel,
		"x-request-at": req.RequestAt,
	}
}

// Ok builds 200 envelope code "00" — hides Gson-equivalent json marshaling via Go's json.
func (p *Platform) Ok(req RequestInfo, body any) ResponseInfo {
	return ResponseInfo{
		Status:  200,
		Headers: headerMap(req),
		Body:    map[string]any{"code": "00", "status": "ok", "message": "request-success", "data": body},
	}
}

// Accepted builds 202 envelope.
func (p *Platform) Accepted(req RequestInfo) ResponseInfo {
	return ResponseInfo{
		Status:  202,
		Headers: headerMap(req),
		Body:    map[string]any{"code": "00", "status": "accepted", "message": "Request being processed"},
	}
}

// Fail builds error envelope — respects lifecycle.ApiError codes 400/401/404/409→01, 429→40, 500→99
func (p *Platform) Fail(req RequestInfo, err error) ResponseInfo {
	msg := "failed"
	if err != nil {
		msg = err.Error()
	}
	status := 500
	code := "99"
	var ae *lifecycle.ApiError
	if errors.As(err, &ae) {
		status = ae.Code
		if ae.Code == 429 {
			code = "40"
		} else if ae.Code != 500 {
			code = "01"
		}
	}
	return ResponseInfo{
		Status:  status,
		Headers: headerMap(req),
		Body:    map[string]any{"code": code, "status": "failed", "message": msg},
	}
}

// Publish inserts ServiceLog-equivalent into outbox via store.Store — hides ServiceLog building and JSON marshaling.
// Unlike operation.Operation which publishes simple op/status, Platform builds richer log with URI/method/host/query.
func (p *Platform) Publish(ctx context.Context, req RequestInfo, resp ResponseInfo) error {
	if p.Store == nil {
		return nil
	}
	// Build ServiceLog payload — mirrors LogsUtils.construcInsertLogs but minimal
	logPayload := map[string]any{
		"id":               uuid.NewString(),
		"appName":          req.AppName,
		"channel":          req.Channel,
		"operationName":    req.OpName,
		"requestId":        req.RequestID,
		"correlationId":    req.CorrelationID,
		"requestAt":        req.RequestAt,
		"requestPayload":   req.Payload,
		"responsePayload":  resp.Body,
		"completionStatus": resp.Body, // caller can extract .status if needed; keep raw for now
		"httpStatusCode":   resp.Status,
		"uri":              req.URI,
		"method":           req.Method,
		"host":             req.Host,
		"queryParam":       req.Query,
	}
	b, err := json.Marshal(logPayload)
	if err != nil {
		return err
	}
	ob := &store.Outbox{
		ID:          uuid.NewString(),
		AggregateID: req.RequestID,
		Topic:       "servicelogs",
		Payload:     b,
		CreatedAt:   p.now(),
	}
	return p.Store.InsertOutbox(ctx, ob)
}
