CREATE SCHEMA IF NOT EXISTS transaction;
CREATE TABLE IF NOT EXISTS transaction.product_trx
(
    id character varying(100) COLLATE pg_catalog."default",
    sys_creation_date timestamp without time zone,
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
    sys_update_date timestamp without time zone,
    payment_date timestamp without time zone,
    discount_enabled boolean,
    discount numeric(3,2)
);

CREATE SCHEMA IF NOT EXISTS store;
CREATE TABLE IF NOT EXISTS store.product
(
    productid integer,
    productstatus boolean,
    productcode character varying(50) COLLATE pg_catalog."default",
    productname character varying(50) COLLATE pg_catalog."default",
    price integer,
    discount numeric(3,2),
    enablediscount boolean,
    specialproduct boolean,
    productinsertdate character varying(50) COLLATE pg_catalog."default",
    productupdatedate character varying(50) COLLATE pg_catalog."default"
);

-- DROP TABLE IF EXISTS store.store_user;

CREATE TABLE IF NOT EXISTS store.store_user
(
    id integer,
    userid character varying(50) COLLATE pg_catalog."default",
    username character varying(50) COLLATE pg_catalog."default",
    firstname character varying(50) COLLATE pg_catalog."default",
    lastname character varying(50) COLLATE pg_catalog."default",
    email character varying(50) COLLATE pg_catalog."default",
    password character varying(100) COLLATE pg_catalog."default",
    specialproduct character varying(50) COLLATE pg_catalog."default",
    recurring character varying(50) COLLATE pg_catalog."default",
    token character varying(40) COLLATE pg_catalog."default"
);

--store.user_product
-- pass: b2xrasd_klhomme0
-- pass: rebajea_ewhicher1
insert into store.store_user (id, userid, username, firstname, lastname, email, password, specialproduct, recurring, token) values (1, 'b2xrasd', 'klhomme0', 'Kimbra', 'L''Homme', 'klhomme0@scribd.com', 'mO6Wb9gyUkMw1NohBtH8SKQSj21cBqj/h0666A5dKqQ=', true, true, '27c8cf05-8354-4b65-89b1-f8b82a615341');
-- pass:
insert into store.store_user (id, userid, username, firstname, lastname, email, password, specialproduct, recurring, token) values (2, 'rebajea', 'ewhicher1', 'Elora', 'Whicher', 'ewhicher1@gov.uk', 'E40k4bmmx1Ke9wGbNZmyOSPeX+vwvazYPKw/WrJpN8k=', false, false, '4d89ff31-a9c7-4e97-aa1d-484145ec34a2');
insert into store.store_user (id, userid, username, firstname, lastname, email, password, specialproduct, recurring, token) values (3, '7qr8lsq', 'jdecreuze2', 'Jasper', 'Decreuze', 'jdecreuze2@reverbnation.com', 'xP8GRCn7vLvTLfUfNAP5KvLk81gQrmtyJ5StscvtKd8=', true, true, 'be5b9997-237d-45e8-ad64-ae35f14bbd9f');
insert into store.store_user (id, userid, username, firstname, lastname, email, password, specialproduct, recurring, token) values (4, 'v3jjsv0', 'ctallman3', 'Cornell', 'Tallman', 'ctallman3@narod.ru', 'uHft52wHb3s1AFpTsIkDbQg5pj4GUq9CuG5ggkge/aw=', false, true, 'b3dcb83d-36f5-4470-a5fd-af4d7c5cb524');
insert into store.store_user (id, userid, username, firstname, lastname, email, password, specialproduct, recurring, token) values (5, 'tf0x3xc', 'cbaistow4', 'Colline', 'Baistow', 'cbaistow4@cnet.com', '2aH5VX5wycptwq4/V1bpDW3Myutsdn2VPrrKjQs62Bc=', false, true, 'e20069e8-4b53-40f3-b909-aa0c0065bb18');
--store.product
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (1, false, 'LOU-60765', 'Beans - Fava, Canned', 49383, 0.98, true, true, '2022-11-06 11:28:27', '2023-04-11 22:22:55');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (2, false, 'TVW-44725', 'Cheese - Parmigiano Reggiano', 19959, 0.64, false, false, '2023-07-03 06:39:20', '2022-10-30 06:22:01');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (3, true, 'TJX-99896', 'Carbonated Water - Blackcherry', 61557, 0.71, false, true, '2022-11-11 23:44:44', '2022-11-30 05:04:33');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (4, true, 'IDM-44572', 'Radish - Pickled', 11362, 0.3, true, false, '2022-08-30 22:03:40', '2023-06-15 11:18:12');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (5, false, 'POH-35224', 'Crab - Meat Combo', 52459, 0.07, false, false, '2023-05-05 22:10:14', '2023-03-09 06:59:25');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (6, true, 'BUM-24071', 'Wine - Red, Black Opal Shiraz', 45647, 0.87, true, false, '2023-05-27 06:56:15', '2022-08-15 15:33:04');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (7, false, 'OZD-61051', 'Mace Ground', 69027, 0.36, true, false, '2022-11-08 06:29:38', '2022-11-14 10:20:35');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (8, false, 'IMQ-09680', 'Scallops 60/80 Iqf', 51042, 0.31, true, true, '2023-03-18 09:15:40', '2022-11-20 07:07:39');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (9, true, 'PAD-05945', 'Puree - Kiwi', 82270, 0.89, false, true, '2023-02-02 20:23:04', '2022-09-12 06:05:01');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (10, false, 'VPY-40184', 'Bread - Bistro White', 47109, 0.41, false, true, '2022-12-04 14:50:53', '2022-11-19 16:09:09');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (11, false, 'QON-42377', 'Wine - White, Gewurtzraminer', 45483, 0.62, true, false, '2022-08-19 08:24:49', '2023-05-22 10:40:38');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (12, false, 'XHE-11954', 'Kellogs Special K Cereal', 36329, 0.82, false, true, '2022-11-28 20:34:57', '2022-12-23 12:25:25');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (13, false, 'FAW-07981', 'Cheese - Provolone', 82555, 0.12, true, true, '2022-10-28 10:39:31', '2022-10-27 05:01:42');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (14, true, 'NZH-97257', 'Isomalt', 95923, 0.66, true, false, '2023-06-20 12:34:47', '2023-04-03 06:20:40');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (15, false, 'FMI-80909', 'Shark - Loin', 41747, 0.2, true, true, '2023-05-27 11:46:01', '2022-09-23 17:20:53');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (16, false, 'AYC-20150', 'Bacardi Limon', 83424, 0.34, false, false, '2022-10-04 10:21:18', '2022-12-31 19:46:48');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (17, false, 'MRB-90475', 'Puree - Mocha', 84560, 0.13, false, true, '2022-08-21 05:40:45', '2023-03-15 22:29:19');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (18, false, 'DVH-69452', 'Pepper - Red, Finger Hot', 6886, 0.34, true, false, '2023-04-14 20:03:40', '2023-01-16 01:47:49');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (19, false, 'HWX-96132', 'Orange - Canned, Mandarin', 95646, 0.9, false, true, '2023-04-19 14:38:19', '2022-12-16 10:35:25');
insert into store.product (productid, productstatus, productcode, productname, price, discount, enablediscount, specialproduct, productinsertdate, productupdatedate) values (20, true, 'GDL-75914', 'Pepper - Red Bell', 86744, 0.49, false, true, '2023-05-02 22:14:51', '2023-03-09 19:27:35');