package com.example.msinvoice.config.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Configuration
public class AppProperties {

    private String APPLICATION_NAME = "ms-invoice";

    private boolean PUBLISH_LOG_KAFKA = true;
    // kafka
    private String SERVICELOG_KAFKA_TOPIC = "ms-event-log";
    private String SERVICELOG_KAFKA_GROUPID = "event-payment-group";

    // status
    private String ORDER_STATUS_PUBLISHED = "PUBLISHED";
    private String PAYMENT_STATUS_SUCCESS = "PUBLISHED";

    // ms db
    private String QUERY_GET_ALL_PRODUCT = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT ";
    private String QUERY_GET_AVAILABLE_PRODUCT = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT WHERE PRODUCTSTATUS is true";
    private String QUERY_GET_USER_DETAIL = "SELECT ID, USERID, USERNAME, FIRSTNAME, LASTNAME, EMAIL, PASSWORD, SPECIALPRODUCT, RECURRING, TOKEN FROM STORE.STORE_USER WHERE USERNAME = ? ";
    private String QUERY_GET_SINGLE_PRODUCT = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT WHERE PRODUCTCODE = ? AND PRODUCTSTATUS is true ";
    private String QUERY_GET_SPECIFIED_PRODUCT = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT WHERE SPECIALPRODUCT is {boolean} AND PRODUCTSTATUS is true ";

    // log db
    private String QUERY_INSERT_PRODUCT_TRX = "INSERT INTO transaction.product_trx(id, sys_creation_date, transactionid, orderstatus, paymentstatus, userid, productname, amount, price, pricecharge, productcode, param_1, param_2, sys_update_date, payment_date, discount_enabled, discount) VALUES (:id, localtimestamp, :transactionid, :orderstatus, :paymentstatus, :userid , :productname, :amount, :price, :pricecharge, :productcode, :param1, :param2, null, null, :discEnabled, :discount)";
    private String QUERY_GET_PRODUCT_TRX_BY_TRANSACTIONID = "SELECT id, sys_creation_date, transactionid, orderstatus, paymentstatus, userid, productname, amount, price, pricecharge, productcode, param_1, param_2, sys_update_date, payment_date, discount_enabled, discount FROM transaction.product_trx where transactionid = :transactioid ";

    // Query
    private String QUERY_SELECT_PRODUCT_TRX_BY_ID = "SELECT id, sys_creation_date, transactionid, orderstatus, paymentstatus, userid, productname, amount, price, pricecharge, productcode, param_1, param_2, sys_update_date, payment_date, discount_enabled, discount FROM transaction.product_trx where transactionid = ? ";
    private String QUERY_UPDATE_PRODUCT_STATUS_TRX_BY_ID = "UPDATE transaction.product_trx set paymentstatus = ?, orderstatus = ? , sys_update_date = localtimestamp where transactionid = ? ";



}
