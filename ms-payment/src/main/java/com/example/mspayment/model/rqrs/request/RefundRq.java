package com.example.mspayment.model.rqrs.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RefundRq {

    private String transactionId;
    private String userId;
}
