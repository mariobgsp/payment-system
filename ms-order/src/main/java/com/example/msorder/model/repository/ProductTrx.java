package com.example.msorder.model.repository;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProductTrx {

    private String id;
    private String sysCreationDate;
    private String transactionId;
    private String orderStatus;
    private String paymentStatus;
    private String userId;
    private String productName;
    private Long amount;
    private Long price;
    private Long priceCharge;
    private String productCode;
    private String param_1;
    private String param_2;
    private String sysUpdateDate;
    private String paymentDate;
    private boolean discountEnabled;
    private Double discount;



}
