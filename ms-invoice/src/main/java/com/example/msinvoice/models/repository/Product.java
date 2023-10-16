package com.example.msinvoice.models.repository;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Product {

    private int productId;
    private boolean productStatus;
    private String productCode;
    private String productName;
    private Integer price;
    private double discount;
    private boolean enableDiscount;
    private boolean specialProductLabel;
    private String productUpdate;
    private String productInsert;
}
