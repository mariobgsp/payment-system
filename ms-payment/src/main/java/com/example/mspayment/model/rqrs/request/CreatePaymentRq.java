package com.example.mspayment.model.rqrs.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreatePaymentRq {

    private String callbackUrl;

}
