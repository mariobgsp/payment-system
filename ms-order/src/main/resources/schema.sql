create SCHEMA IF NOT EXISTS store;

create table store.store_user (
	id INT,
	userid VARCHAR(50),
	username VARCHAR(50),
	firstname VARCHAR(50),
	lastname VARCHAR(50),
	email VARCHAR(50),
	password VARCHAR(100),
	specialproduct VARCHAR(50),
	recurring VARCHAR(50),
	token VARCHAR(40)
);

create table store.product (
	productid INT,
	productstatus VARCHAR(50),
	productcode VARCHAR(50),
	productname VARCHAR(50),
	price INT,
	discount DECIMAL(3,2),
	enablediscount VARCHAR(50),
	specialproduct VARCHAR(50),
	productinsertdate VARCHAR(50),
	productupdatedate VARCHAR(50)
);