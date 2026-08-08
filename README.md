# payment-system

## Overview

Microservices-based payment system with a modern web frontend:

* **frontend** - Next.js (App Router) web portal: login, product catalog, order placement, payment checkout, status tracking and refunds
* **ms-order** - Java/Spring: user auth (session tokens), product inquiry and order creation
* **ms-payment** - Java/Spring: payment creation, refunds and secure payment callbacks
* **ms-paymentagr** - Go/Gin + Redis: dummy payment partner (charge, refund, redirect/checkout page)
* **ms-invoice** - Java/Spring: consumes payment events from Kafka and provisions PDF invoice reports
* **ms-logger** - Java/Spring: consumes the `ms-event-log` topic and persists service logs to MongoDB

## Tech Stack

* Java Spring Boot 17 - main framework (ms-order, ms-payment, ms-logger, ms-invoice)
* PostgreSQL - transactional databases (`ms`, `transaction`)
* MongoDB - service log storage (ms-logger)
* Kafka - event streaming between services
* Go + Gin + Redis - ms-paymentagr (payment partner simulation)
* Next.js 16 + TypeScript + Tailwind CSS - web frontend
* Docker Compose - full-stack local deployment

## Getting Started

### 1. Prerequisites

* Docker with Docker Compose v2
* Node.js 20+ and npm (only for frontend development outside Docker)

### 2. Configure environment

```bash
cd project
cp .env.example .env
# EDIT .env and change every secret before deploying
```

### 3. Build and start all services

```bash
docker compose up --build
```

This seeds PostgreSQL (schemas + demo users/products) on first boot, then starts all services.

### 4. Access the system

* **Web portal**: http://localhost:3000
* **ms-order** (API): http://localhost:8080 - Swagger UI at `/swagger-ui.html`
* **ms-payment** (API): http://localhost:9090
* **ms-paymentagr**: http://localhost:8081 (`/health`)
* **ms-invoice**: http://localhost:8082
* **ms-logger**: http://localhost:1337 (`/ms/api/v1/health/check`)
* **kafka-ui**: http://localhost:8090

### 5. Demo accounts

Seeded in PostgreSQL on first boot (see `project/pg-init-scripts/sql/20-store-schema.sql`):

| Username   | Password      | Access            |
|------------|---------------|-------------------|
| `klhomme0` | `user1Pass!`  | special products  |
| `ewhicher1`| `user2Pass!`  | standard products |
| `jdecreuze2`| `user3Pass!` | special products  |
| `admin1`   | `adminPass!`  | special products  |

### 6. End-to-end flow

1. Sign in on the web portal
2. Pick a product from the catalog, choose quantity, place the order
3. Click **Create payment** to get a checkout URL from ms-paymentagr
4. Open the payment page and confirm (simulates the payment partner)
5. The portal polls the transaction status; on success ms-invoice provisions the invoice PDF

### 7. Stopping the services

```bash
docker compose down
```

## Security

* **Authenticated payment callbacks** - ms-paymentagr signs every callback with HMAC-SHA256 (`NOTIFY_SECRET` + timestamp); ms-payment verifies signature and replay window before accepting
* **API key protection** - ms-paymentagr charge/refund endpoints require the `api-key` header (`PARTNER_API_KEY`)
* **Session-based auth** - ms-order issues short-lived opaque session tokens (in-memory, TTL); order placement accepts `Authorization: Bearer` tokens
* **Password hashing** - BCrypt for new credentials, legacy HMAC verification kept for backward compatibility
* **Login rate limiting** - per user/IP attempts (10 / 15 minutes)
* **No credential leakage** - user detail responses never expose password hashes or stored tokens
* **Parameterized SQL** - all queries use prepared statements; the previous string-replace query pattern was removed
* **Secrets via environment** - no hardcoded credentials in source or compose; `.env` is gitignored, a `.env.example` template is provided
* **Path traversal protection** - invoice filenames sanitize the transaction id
* **Kafka reliability** - consumers acknowledge only after processing (no lost events)
* **Dependency updates** - gson 2.11, gin 1.10, go-redis 9.5, x/net 0.25, JasperReports 6.21.4, Next.js 16 (npm audit clean)
* **Container hardening** - all containers run as non-root users; infra ports bind to `127.0.0.1` only
* **Security headers** - CORS allowlist plus `X-Content-Type-Options`, `X-Frame-Options`, `CSP`, `Referrer-Policy`, `Cache-Control: no-store`
* **Removed attack surface** - unauthenticated log-publish endpoint removed, partner `/payments/test` endpoint removed
* **Live DB data no longer committed** - `project/db-data/` is removed from the repository

## Testing

```bash
# Java services (unit tests)
cd ms-order && ./mvnw test
cd ms-payment && ./mvnw test
cd ms-logger && ./mvnw test
cd ms-invoice && ./mvnw test

# Go service
cd ms-paymentagr && go vet ./... && go build ./...

# Frontend
cd frontend && npm ci && npm run build
```

CI (`.github/workflows/ci-cd-pipeline.yml`) runs all of the above plus full-stack compose health checks on every push to `master`.
