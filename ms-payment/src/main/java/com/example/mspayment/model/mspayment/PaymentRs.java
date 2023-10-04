package com.example.mspayment.model.mspayment;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PaymentRs {

    private String id;
    private String status;
    private String currency;
    private String checkout_method;
    private Long amount;
    private String payment_code;
    private String reference_id;
    private Object channel_properties;
    private Actions action;
    private String callback_url;
    private String created;
    private String updated;

}
