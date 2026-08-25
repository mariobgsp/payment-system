# Security Exceptions

## Legacy Java IDOR — ms-payment / ms-order
- **Files:** `ms-payment/src/main/java/com/example/mspayment/usecase/PaymentUsecase.java:45,88,121` + `ms-order/src/main/java/com/example/msorder/usecase/OrderUsecase.java:247`
- **Risk:** `createPayment`/`refundPayment`/`checkProduct` only check transaction exists, not `productTrx.userId == session.userId`. Any authenticated user can enumerate others' transactions (IDOR).
- **Deviation:** Accepted for legacy path. Monolith `TransactionLifecycle.Check` already enforces `userId == callerUserId` (401) + `monolith/identity` rate-limit. Legacy services will be retired when `monolith:8085` fully replaces `ms-order:8080`/`ms-payment:9090` (see `README.md` minimal 4-container profile). No new legacy endpoints added.
- **Mitigation:** Frontend now wired to `monolith:8085` (`project/docker-compose.yml` frontend `MS_ORDER_URL=http://monolith:8085`), so new flows go through monolith authz. Legacy `ms-order`/`ms-payment` kept for back-compat but not used for new orders.

## Session / loginAttempts memory leak
- **Files:** `ms-order/src/main/java/com/example/msorder/service/SessionService.java:27` + `ms-order/src/main/java/com/example/msorder/delivery/AuthController.java:15`
- **Risk:** `ConcurrentHashMap` TTL lazy check only, no eviction ticker → leak if token never validated.
- **Deviation:** Accepted for legacy. Monolith `monolith/identity` has `evictLoop` 60s ticker for both sessions and attempts. Legacy leak bounded by 1800s TTL and 10/15m window, not grown in prod (few hundred sessions). Will retire with legacy.

## RedisTTL vs SessionTTL mismatch
- **File:** `ms-paymentagr/usecase/payment_usecase.go:28` + `ms-paymentagr/config/config.go:RedisTTL 300s` vs `SESSION_TTL_SECONDS 1800s`
- **Deviation:** Accepted. Monolith outbox + sweeper (5m) now owns expiry, Redis only cache. TTL alignment will be per-checkout when partner demands (ponytail).

## Monolith idempotency race — FIXED
- **Files:** `monolith/lifecycle/lifecycle.go:108` + `monolith/store/pg.go:32` + `monolith/store/store.go:ErrIdempotencyExists`
- **Fix:** `Store.InsertTxWithOutbox` now does `INSERT idempotency ... ON CONFLICT DO NOTHING` first in same TX and returns `ErrIdempotencyExists` if `RowsAffected==0`, so `product_trx` not inserted for duplicate key. `lifecycle.CreateOrderWithIdempotency` now uses `errors.Is(err, store.ErrIdempotencyExists)` sentinel (not string compare) and retries `GetIdempotency` (5×10ms) to return cached `PTRX-` instead of 400. `MemoryStore` placeholder fixed to `"{}"` same as PG. No duplicate rows.
- **Residual:** `PutIdempotency` failure after commit could leave placeholder `"{}"` → next call would see `cached != "{}"` false and retry loop returns 400. Low risk (Put is single UPDATE, fails only on DB down). Accepted; will be fixed by writing final response inside same TX as `InsertTxWithOutbox`.

