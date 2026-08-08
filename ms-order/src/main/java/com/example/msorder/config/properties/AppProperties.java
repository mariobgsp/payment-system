package com.example.msorder.config.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Configuration
public class AppProperties {

    private String APPLICATION_NAME = "ms-order";

    private boolean PUBLISH_LOG_KAFKA = true;

    // MessageDto
    private String SERVICELOG_KAFKA_TOPIC = "ms-event-log";
    private String SERVICELOG_KAFKA_GROUP_ID = "service_log";

    @Value("${app.security.secret-key:bXlvbmx5X3Bhc3N3b3JkOg==}")
    private String SECRET_KEY;
    @Value("${app.session.ttl-seconds:1800}")
    private long SESSION_TTL_SECONDS;
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;
    private String ORDER_STATUS_CREATED = "CREATED";
    private String PAYMENT_STATUS_CREATED = "CREATED";

    // ms db
    private String QUERY_GET_ALL_PRODUCT = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT ";
    private String QUERY_GET_AVAILABLE_PRODUCT = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT WHERE PRODUCTSTATUS is true";
    private String QUERY_GET_USER_DETAIL = "SELECT ID, USERID, USERNAME, FIRSTNAME, LASTNAME, EMAIL, PASSWORD, SPECIALPRODUCT, RECURRING, TOKEN FROM STORE.STORE_USER WHERE USERNAME = ? ";
    private String QUERY_GET_SINGLE_PRODUCT = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT WHERE PRODUCTCODE = ? AND PRODUCTSTATUS is true ";
    private String QUERY_GET_SPECIFIED_PRODUCT = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT WHERE SPECIALPRODUCT = ? AND PRODUCTSTATUS is true ";

    // log db
    private String QUERY_INSERT_PRODUCT_TRX = "INSERT INTO transaction.product_trx(id, sys_creation_date, transactionid, orderstatus, paymentstatus, userid, productname, amount, price, pricecharge, productcode, param_1, param_2, sys_update_date, payment_date, discount_enabled, discount) VALUES (:id, localtimestamp, :transactionid, :orderstatus, :paymentstatus, :userid , :productname, :amount, :price, :pricecharge, :productcode, :param1, :param2, null, null, :discEnabled, :discount)";
}
