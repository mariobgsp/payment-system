package com.example.mspayment.config.properties;

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

    private String APPLICATION_NAME = "ms-payment";

    private boolean PUBLISH_LOG_KAFKA = true;

    // MessageDto
    private String SERVICELOG_KAFKA_TOPIC = "ms-event-log";
    private String SERVICELOG_KAFKA_GROUP_ID = "service_log";

    private String SECRET_KEY = "bXlvbmx5X3Bhc3N3b3JkOg==";
    private String ORDER_STATUS_READY = "READY";
    private String PAYMENT_STATUS_READY = "READY";

    private String ORDER_STATUS_PUBLISHED = "PUBLISHED";
    private String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    private String PAYMENT_STATUS_REFUND = "REFUND";

    private String PAYMENT_SUCCEEDED = "SUCCEEDED";

    // payment service
    private String PAYMENT_CURRENCY = "IDR";
    private String PAYMENT_CHECKOUT_METHOD = "ONE_TIME_PAYMENT";
    private String PAYMENT_CHARGE_URL = "http://ms-paymentagr:8081/payments/charge";
    private String PAYMENT_REFUND_URL = "http://ms-paymentagr:8081/payments/refund";

    // Query
    private String QUERY_SELECT_PRODUCT_TRX_BY_ID = "SELECT id, sys_creation_date, transactionid, orderstatus, paymentstatus, userid, productname, amount, price, pricecharge, productcode, param_1, param_2, sys_update_date, payment_date, discount_enabled, discount FROM transaction.product_trx where transactionid = ? ";
    private String QUERY_UPDATE_PRODUCT_STATUS_TRX_BY_ID = "UPDATE transaction.product_trx set paymentstatus = ?, orderstatus = ? , sys_update_date = localtimestamp where transactionid = ? ";
}
