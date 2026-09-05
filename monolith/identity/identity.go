package identity

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"strconv"
	"sync"
	"time"

	"payment-system/monolith/store"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
)

// Clock seam — SystemClock vs FakeClock (deterministic TTL/replay)
type Clock interface {
	Now() time.Time
}
type SystemClock struct{}

func (SystemClock) Now() time.Time { return time.Now() }

type FakeClock struct{ T time.Time }

func (f *FakeClock) Now() time.Time          { return f.T }
func (f *FakeClock) Advance(d time.Duration) { f.T = f.T.Add(d) }

// Session mirrors store user session
type Session struct {
	Token     string
	UserID    string
	Username  string
	CreatedAt time.Time
}

type loginAttempt struct {
	count          int
	firstAttemptAt time.Time
}

// Identity is deep Module seam — hides rate-limit 10/15m, TTL 1800s eviction, BCrypt vs HMAC branch, 300s replay
type Identity struct {
	store       store.Store
	secretKey   string // APP_SECRET_KEY for legacy HMAC
	ttl         time.Duration
	maxAttempts int
	window      time.Duration
	tolerance   time.Duration
	clock       Clock

	mu       sync.Mutex
	sessions map[string]*Session
	attempts map[string]*loginAttempt
}

func (id *Identity) StoreUser(ctx context.Context, username string) (*store.StoreUser, error) {
	return id.store.GetUserDetail(ctx, username)
}
func NewIdentity(s store.Store, ttl time.Duration, maxAttempts int, window time.Duration, tolerance time.Duration, clock Clock, secretKey string) *Identity {
	if ttl == 0 {
		ttl = 1800 * time.Second
	}
	if maxAttempts == 0 {
		maxAttempts = 10
	}
	if window == 0 {
		window = 15 * time.Minute
	}
	if tolerance == 0 {
		tolerance = 300 * time.Second
	}
	if clock == nil {
		clock = SystemClock{}
	}
	id := &Identity{
		store: s, secretKey: secretKey,
		ttl: ttl, maxAttempts: maxAttempts, window: window, tolerance: tolerance, clock: clock,
		sessions: make(map[string]*Session), attempts: make(map[string]*loginAttempt),
	}
	go id.evictLoop()
	return id
}

func (id *Identity) evictLoop() {
	t := time.NewTicker(60 * time.Second)
	defer t.Stop()
	for range t.C {
		now := id.clock.Now()
		id.mu.Lock()
		for tok, sess := range id.sessions {
			if now.Sub(sess.CreatedAt) > id.ttl {
				delete(id.sessions, tok)
			}
		}
		for k, a := range id.attempts {
			if now.Sub(a.firstAttemptAt) > id.window {
				delete(id.attempts, k)
			}
		}
		id.mu.Unlock()
	}
}

// Login — rate-limit per username|IP hidden, BCrypt vs legacy HMAC hidden
func (id *Identity) Login(ctx context.Context, username, password, clientIP string) (Session, error) {
	key := username + "|" + clientIP
	if !id.allowAttempt(key) {
		return Session{}, ErrTooManyAttempts
	}
	if username == "" || password == "" {
		return Session{}, ErrBadRequest
	}
	u, err := id.store.GetUserDetail(ctx, username)
	if err != nil {
		id.recordFailure(key)
		return Session{}, ErrInvalidCredentials
	}
	if !VerifyPassword(password, u.Password, id.secretKey) {
		id.recordFailure(key)
		return Session{}, ErrInvalidCredentials
	}
	// success — clear attempts
	id.mu.Lock()
	delete(id.attempts, key)
	sess := &Session{Token: uuid.NewString(), UserID: u.UserID, Username: u.Username, CreatedAt: id.clock.Now()}
	id.sessions[sess.Token] = sess
	id.mu.Unlock()
	return *sess, nil
}

func (id *Identity) Validate(ctx context.Context, token string) (Session, error) {
	if token == "" {
		return Session{}, ErrInvalidToken
	}
	id.mu.Lock()
	defer id.mu.Unlock()
	sess, ok := id.sessions[token]
	if !ok {
		return Session{}, ErrInvalidToken
	}
	if id.clock.Now().Sub(sess.CreatedAt) > id.ttl {
		delete(id.sessions, token)
		return Session{}, ErrInvalidToken
	}
	return *sess, nil
}

func (id *Identity) Logout(ctx context.Context, token string) error {
	if token == "" {
		return nil
	}
	id.mu.Lock()
	delete(id.sessions, token)
	id.mu.Unlock()
	return nil
}

// VerifyCallback — 300s replay + hmac.Equal hidden
func (id *Identity) VerifyCallback(secret, timestamp, body, signature string) bool {
	if secret == "" || timestamp == "" || body == "" || signature == "" {
		return false
	}
	// replay window
	ts, err := parseTimestamp(timestamp)
	if err != nil {
		return false
	}
	if absDuration(id.clock.Now().Sub(ts)) > id.tolerance {
		return false
	}
	expected := hmacSha256Hex(secret, timestamp, body)
	return hmac.Equal([]byte(expected), []byte(signature))
}

func parseTimestamp(ts string) (time.Time, error) {
	for _, layout := range []string{time.RFC3339, time.RFC1123, "2006-01-02T15:04:05Z07:00"} {
		if t, err := time.Parse(layout, ts); err == nil {
			return t, nil
		}
	}
	sec, err := strconv.ParseInt(ts, 10, 64)
	if err != nil {
		return time.Time{}, err
	}
	return time.Unix(sec, 0), nil
}

func absDuration(d time.Duration) time.Duration {
	if d < 0 {
		return -d
	}
	return d
}

// helpers — mirrored from ms-order/ms-payment SecurityUtils

func VerifyPassword(rawPassword, storedHash, secret string) bool {
	if rawPassword == "" || storedHash == "" {
		return false
	}
	if len(storedHash) > 2 && storedHash[:3] == "$2a" || len(storedHash) > 2 && storedHash[:3] == "$2b" {
		if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(rawPassword)); err == nil {
			return true
		}
		return false
	}
	// legacy HMAC-SHA256 base64
	expected := encodeRequestBody(rawPassword, secret)
	if expected == "" {
		return false
	}
	return hmac.Equal([]byte(expected), []byte(storedHash))
}

func encodeRequestBody(requestBody, secret string) string {
	if secret == "" {
		return ""
	}
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(requestBody))
	return base64.StdEncoding.EncodeToString(mac.Sum(nil))
}

func hmacSha256Hex(secret, timestamp, payload string) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(timestamp))
	mac.Write([]byte(payload))
	return hex.EncodeToString(mac.Sum(nil))
}

var (
	ErrBadRequest         = &ApiError{Code: 400, Msg: "bad request"}
	ErrInvalidCredentials = &ApiError{Code: 401, Msg: "invalid username or password"}
	ErrInvalidToken       = &ApiError{Code: 401, Msg: "invalid token"}
	ErrTooManyAttempts    = &ApiError{Code: 429, Msg: "too many login attempts, try again later"}
)

type ApiError struct {
	Code int
	Msg  string
}

func (e *ApiError) Error() string { return e.Msg }

func (id *Identity) allowAttempt(key string) bool {
	id.mu.Lock()
	defer id.mu.Unlock()
	a, ok := id.attempts[key]
	if !ok {
		return true
	}
	if id.clock.Now().Sub(a.firstAttemptAt) > id.window {
		delete(id.attempts, key)
		return true
	}
	return a.count < id.maxAttempts
}
func (id *Identity) recordFailure(key string) {
	id.mu.Lock()
	defer id.mu.Unlock()
	now := id.clock.Now()
	if a, ok := id.attempts[key]; ok && now.Sub(a.firstAttemptAt) <= id.window {
		a.count++
	} else {
		id.attempts[key] = &loginAttempt{count: 1, firstAttemptAt: now}
	}
}
