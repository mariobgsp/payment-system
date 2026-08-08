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

-- Demo users (BCrypt hashed passwords):
--   klhomme0  / user1Pass!  (special product user)
--   ewhicher1 / user2Pass!
--   jdecreuze2 / user3Pass!  (special product user)
--   admin1    / adminPass!   (special product user)
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
    (7, false, 'OZD-61051', 'Mace Ground', 69027, 0.36, true, false, '2022-11-08 06:29:38', '2022-11-14 10:20:35'),
    (8, false, 'IMQ-09680', 'Scallops 60/80 Iqf', 51042, 0.31, true, true, '2023-03-18 09:15:40', '2022-11-20 07:07:39'),
    (9, true, 'PAD-05945', 'Puree - Kiwi', 82270, 0.89, false, true, '2023-02-02 20:23:04', '2022-09-12 06:05:01'),
    (10, false, 'VPY-40184', 'Bread - Bistro White', 47109, 0.41, false, true, '2022-12-04 14:50:53', '2022-11-19 16:09:09'),
    (11, false, 'QON-42377', 'Wine - White, Gewurtzraminer', 45483, 0.62, true, false, '2022-08-19 08:24:49', '2023-05-22 10:40:38'),
    (12, false, 'XHE-11954', 'Kellogs Special K Cereal', 36329, 0.82, false, true, '2022-11-28 20:34:57', '2022-12-23 12:25:25'),
    (13, false, 'FAW-07981', 'Cheese - Provolone', 82555, 0.12, true, true, '2022-10-28 10:39:31', '2022-10-27 05:01:42'),
    (14, true, 'NZH-97257', 'Isomalt', 95923, 0.66, true, false, '2023-06-20 12:34:47', '2023-04-03 06:20:40'),
    (15, false, 'FMI-80909', 'Shark - Loin', 41747, 0.2, true, true, '2023-05-27 11:46:01', '2022-09-23 17:20:53'),
    (16, false, 'AYC-20150', 'Bacardi Limon', 83424, 0.34, false, false, '2022-10-04 10:21:18', '2022-12-31 19:46:48'),
    (17, false, 'MRB-90475', 'Puree - Mocha', 84560, 0.13, false, true, '2022-08-21 05:40:45', '2023-03-15 22:29:19'),
    (18, false, 'DVH-69452', 'Pepper - Red, Finger Hot', 6886, 0.34, true, false, '2023-04-14 20:03:40', '2023-01-16 01:47:49'),
    (19, false, 'HWX-96132', 'Orange - Canned, Mandarin', 95646, 0.9, false, true, '2023-04-19 14:38:19', '2022-12-16 10:35:25'),
    (20, true, 'GDL-75914', 'Pepper - Red Bell', 86744, 0.49, false, true, '2023-05-02 22:14:51', '2023-03-09 19:27:35')
ON CONFLICT (productid) DO NOTHING;
