-- 001_lifecycle.sql — replaces Kafka+Mongo with PG outbox+idempotency.
-- Extends transaction.product_trx (21-transaction-schema.sql) — no drop.

CREATE SCHEMA IF NOT EXISTS transaction;

-- outbox replaces ms-notify-payment + servicelogs topics
CREATE TABLE IF NOT EXISTS transaction.outbox (
    id            varchar(100) NOT NULL PRIMARY KEY,
    aggregate_id  varchar(100) NOT NULL,
    topic         varchar(100) NOT NULL,
    payload       jsonb        NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    processed_at  timestamptz
);
CREATE INDEX IF NOT EXISTS idx_outbox_unprocessed ON transaction.outbox (created_at) WHERE processed_at IS NULL;

-- idempotency replaces Redis TTL dup guard — also caches CreateOrder response
CREATE TABLE IF NOT EXISTS transaction.idempotency (
    key        varchar(256) NOT NULL PRIMARY KEY,
    response   jsonb        NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz  NOT NULL DEFAULT now()
);

-- help sweeper: PENDING/READY stale scan
CREATE INDEX IF NOT EXISTS idx_product_trx_stale ON transaction.product_trx (paymentstatus, sys_creation_date);
