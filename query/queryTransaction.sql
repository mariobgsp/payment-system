SELECT * FROM transaction.product_trx order by sys_creation_date desc;

DROP TABLE transaction.product_trx;

CREATE TABLE IF NOT EXISTS transaction.product_trx
(
    id character varying(100) COLLATE pg_catalog."default",
    sys_creation_date date,
    transactionid character varying(100) COLLATE pg_catalog."default",
	orderstatus character varying(25) COLLATE pg_catalog."default",
	paymentstatus character varying(25) COLLATE pg_catalog."default",
    userid character varying(50) COLLATE pg_catalog."default",
    productname character varying(50) COLLATE pg_catalog."default",
    amount numeric,
    price numeric,
    pricecharge numeric,
    productcode character varying(50) COLLATE pg_catalog."default",
    param_1 character varying(50) COLLATE pg_catalog."default",
    param_2 character varying(50) COLLATE pg_catalog."default",
    sys_update_date date,
    payment_date date,
    discount_enabled boolean,
    discount numeric(3,2)
)