package com.example.msorder.model.rqrs.request.order;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OrderRq {

    private String productCode;
    private String productName;
    private int amount;
    private int price;
    private UserDetail userDetail;
    private boolean enableDiscount;
}
