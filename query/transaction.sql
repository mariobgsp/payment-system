-- Canonical schema for the transaction database.
-- Applied automatically on first boot via
-- project/pg-init-scripts/10-init-databases.sh
CREATE SCHEMA IF NOT EXISTS transaction;

CREATE SEQUENCE IF NOT EXISTS transaction.product_trx_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS transaction.product_trx
(
    id varchar(100) NOT NULL PRIMARY KEY,
    sys_creation_date timestamp,
    transactionid varchar(100),
    orderstatus varchar(25),
    paymentstatus varchar(25),
    userid varchar(50),
    productname varchar(50),
    amount bigint,
    price bigint,
    pricecharge bigint,
    productcode varchar(50),
    param_1 varchar(50),
    param_2 varchar(50),
    sys_update_date timestamp,
    payment_date timestamp,
    discount_enabled boolean,
    discount numeric(3,2)
);
