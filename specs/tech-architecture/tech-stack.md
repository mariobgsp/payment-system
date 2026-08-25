---
type: tech-stack
context: payment-system
---

# Tech Stack — payment-system

Domain language — canonical terms for seams.

| Term | Meaning |
| ------ | --------- |
| TransactionLifecycle | Deep Module owning `product_trx` status chart + pricing + idempotency + authz. Single seam for order→charge→callback→refund. |
| Product | `store.product` — code, price, discount, enableDiscount, specialProduct |
| StoreUser | `store.store_user` — auth principal, BCrypt password, specialProduct flag |
| ProductTrx | `transaction.product_trx` — id, transactionId `PTRX-`, orderStatus, paymentStatus, priceCharge, userId |
| Charge | PaymentAdapter request via `paymentagr` — referenceId=transactionId, amount=priceCharge |
| Callback | `POST /v1/payment/notify` HMAC event — `SUCCEEDED` → `PUBLISHED`/`SUCCESS` |
| Outbox | PG table mirroring `ms-notify-payment` + `servicelogs` — atomic TX + poller, replaces Kafka+ZK+Mongo |
| IdempotencyKey | Client `Idempotency-Key` header for `CreateOrder`; `transactionId` as natural key for `Charge`/`Refund` |

Influences seams: TransactionLifecycle is In-process seam, internal repo seam is Local-substitutable (PG → in-memory fake).
