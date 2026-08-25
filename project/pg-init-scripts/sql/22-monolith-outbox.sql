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

-- store schema in transaction DB for monolith single-DSN (so monolith can query products/users via one pool)
-- ponytail: duplicate of 20-store-schema applied to transaction DB; consolidate to one DB when legacy ms-order retired
CREATE SCHEMA IF NOT EXISTS store;
CREATE TABLE IF NOT EXISTS store.product
(
    productid integer PRIMARY KEY,
    productstatus boolean,
    productcode character varying(50),
    productname character varying(50),
    price integer,
    discount numeric(3,2),
    enablediscount boolean,
    specialproduct boolean,
    productinsertdate character varying(50),
    productupdatedate character varying(50)
);
CREATE TABLE IF NOT EXISTS store.store_user
(
    id integer PRIMARY KEY,
    userid character varying(50),
    username character varying(50),
    firstname character varying(50),
    lastname character varying(50),
    email character varying(50),
    password character varying(100),
    specialproduct boolean DEFAULT false,
    recurring boolean DEFAULT false,
    token character varying(40)
);
INSERT INTO store.store_user (id, userid, username, firstname, lastname, email, password, specialproduct, recurring, token) VALUES
    (1, 'b2xrasd', 'klhomme0', 'Kimbra', 'L''Homme', 'klhomme0@scribd.com', '$2a$10$hKEM57bZ02TVXn4oYGH3C.QpYrO5OKqawNgfCg.pJyzyWxCgxG1rm', true, true, '27c8cf05-8354-4b65-89b1-f8b82a615341'),
    (2, 'rebajea', 'ewhicher1', 'Elora', 'Whicher', 'ewhicher1@gov.uk', '$2a$10$jduyUW35N8uHdfwgD.R8uuSYYOD0sxF58Y9f5UpW2iCd06pQD5gYS', false, false, '4d89ff31-a9c7-4e97-aa1d-484145ec34a2'),
    (3, '7qr8lsq', 'jdecreuze2', 'Jasper', 'Decreuze', 'jdecreuze2@reverbnation.com', '$2a$10$zeIpfM5NWV7xH/gGe..7YePqUl2B.le3TELDoIp0JXo76ly8lxGZO', true, true, 'be5b9997-237d-45e8-ad64-ae35f14bbd9f'),
    (4, 'v3jjsv0', 'admin1', 'Admin', 'User', 'admin1@example.com', '$2a$10$TjG35AEva9aTKphO8zsLe.7tZ13OXhdQS.g4sdfrXpDIh5SLVhZt.', true, true, 'b3dcb83d-36f5-4470-a5fd-af4d7c5cb524')
ON CONFLICT (id) DO NOTHING;
INSERT INTO store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) VALUES
    (1, false, 'LOU-60765', 'Beans - Fava, Canned', 49383, 0.98, true, true, '2022-11-06 11:28:27', '2023-04-11 22:22:55'),
    (2, false, 'TVW-44725', 'Cheese - Parmigiano Reggiano', 19959, 0.64, false, false, '2023-07-03 06:39:20', '2022-10-30 06:22:01'),
    (3, true, 'TJX-99896', 'Carbonated Water - Blackcherry', 61557, 0.71, false, true, '2022-11-11 23:44:44', '2022-11-30 05:04:33'),
    (4, true, 'IDM-44572', 'Radish - Pickled', 11362, 0.3, true, false, '2022-08-30 22:03:40', '2023-06-15 11:18:12'),
    (5, false, 'POH-35224', 'Crab - Meat Combo', 52459, 0.07, false, false, '2023-05-05 22:10:14', '2023-03-09 06:59:25'),
    (6, true, 'BUM-24071', 'Wine - Red, Black Opal Shiraz', 45647, 0.87, true, false, '2023-05-27 06:56:15', '2022-08-15 15:33:04'),
    (9, true, 'PAD-05945', 'Puree - Kiwi', 82270, 0.89, false, true, '2023-02-02 20:23:04', '2022-09-12 06:05:01'),
    (14, true, 'NZH-97257', 'Isomalt', 95923, 0.66, true, false, '2023-06-20 12:34:47', '2023-04-03 06:20:40'),
    (20, true, 'GDL-75914', 'Pepper - Red Bell', 86744, 0.49, false, true, '2023-05-02 22:14:51', '2023-03-09 19:27:35')
ON CONFLICT (productid) DO NOTHING;
