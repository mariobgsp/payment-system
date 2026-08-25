package identity

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"strconv"
	"testing"
	"time"

	"payment-system/monolith/store"
)

func TestLoginSuccessAndValidate(t *testing.T) {
	mem := store.NewMemoryStore()
	clk := &FakeClock{T: time.Now()}
	id := NewIdentity(mem, 1800*time.Second, 10, 15*time.Minute, 300*time.Second, clk, "test-secret")
	sess, err := id.Login(context.Background(), "klhomme0", "user1Pass!", "127.0.0.1")
	if err != nil {
		t.Fatalf("login: %v", err)
	}
	if sess.Username != "klhomme0" {
		t.Fatalf("username %s", sess.Username)
	}
	got, err := id.Validate(context.Background(), sess.Token)
	if err != nil {
		t.Fatalf("validate: %v", err)
	}
	if got.UserID != sess.UserID {
		t.Fatalf("mismatch")
	}
}

func TestLoginRateLimit(t *testing.T) {
	mem := store.NewMemoryStore()
	clk := &FakeClock{T: time.Now()}
	id := NewIdentity(mem, 1800*time.Second, 2, 15*time.Minute, 300*time.Second, clk, "s")
	// fail twice
	id.Login(context.Background(), "klhomme0", "wrong", "1.1.1.1")
	id.Login(context.Background(), "klhomme0", "wrong", "1.1.1.1")
	_, err := id.Login(context.Background(), "klhomme0", "wrong", "1.1.1.1")
	if err != ErrTooManyAttempts {
		t.Fatalf("want too many, got %v", err)
	}
	// advance window -> allow again
	clk.Advance(16 * time.Minute)
	_, err = id.Login(context.Background(), "klhomme0", "wrong", "1.1.1.1")
	if err == ErrTooManyAttempts {
		t.Fatalf("should allow after window")
	}
}

func TestValidateExpiry(t *testing.T) {
	mem := store.NewMemoryStore()
	clk := &FakeClock{T: time.Now()}
	id := NewIdentity(mem, 2*time.Second, 10, 15*time.Minute, 300*time.Second, clk, "s")
	sess, _ := id.Login(context.Background(), "klhomme0", "user1Pass!", "127.0.0.1")
	clk.Advance(3 * time.Second)
	_, err := id.Validate(context.Background(), sess.Token)
	if err == nil {
		t.Fatal("want expiry")
	}
}

func TestVerifyCallback(t *testing.T) {
	clk := &FakeClock{T: time.Unix(1000, 0)}
	id := NewIdentity(store.NewMemoryStore(), 0, 0, 0, 300*time.Second, clk, "")
	secret := "CHANGE_ME_notify_secret"
	ts := strconv.FormatInt(int64(1000), 10)
	body := `{"referenceId":"PTRX-1"}`
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(ts))
	mac.Write([]byte(body))
	sig := hex.EncodeToString(mac.Sum(nil))
	if !id.VerifyCallback(secret, ts, body, sig) {
		t.Fatal("want valid")
	}
	// replay beyond 300s
	clk.Advance(400 * time.Second)
	if id.VerifyCallback(secret, ts, body, sig) {
		t.Fatal("want reject replay")
	}
	// bad sig
	if id.VerifyCallback(secret, strconv.FormatInt(int64(1000), 10), body, "bad") {
		t.Fatal("want reject bad sig")
	}
}

func TestVerifyPasswordLegacy(t *testing.T) {
	secret := "test-secret"
	// bcrypt path
	if !VerifyPassword("user1Pass!", "$2a$10$hKEM57bZ02TVXn4oYGH3C.QpYrO5OKqawNgfCg.pJyzyWxCgxG1rm", secret) {
		t.Fatal("bcrypt verify failed")
	}
	if VerifyPassword("wrong", "$2a$10$hKEM57bZ02TVXn4oYGH3C.QpYrO5OKqawNgfCg.pJyzyWxCgxG1rm", secret) {
		t.Fatal("should fail wrong")
	}
	// legacy HMAC path — generate expected and verify
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte("legacyPass"))
	legacy := string(encodeRequestBody("legacyPass", secret))
	if !VerifyPassword("legacyPass", legacy, secret) {
		t.Fatal("legacy verify failed")
	}
}

func TestLogout(t *testing.T) {
	mem := store.NewMemoryStore()
	id := NewIdentity(mem, 0, 0, 0, 0, &FakeClock{T: time.Now()}, "")
	sess, _ := id.Login(context.Background(), "klhomme0", "user1Pass!", "127.0.0.1")
	id.Logout(context.Background(), sess.Token)
	_, err := id.Validate(context.Background(), sess.Token)
	if err == nil {
		t.Fatal("want invalid after logout")
	}
}
