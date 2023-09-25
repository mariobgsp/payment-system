package com.example.mspayment.model.mspayment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PaymentRq {
    @JsonProperty("reference_id")
    private String referenceId;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("amount")
    private Long amount;
    @JsonProperty("checkout_method")
    private String checkoutMethod;
    @JsonProperty("payment_code")
    private String paymentCode;
    @JsonProperty("redirect_url")
    private String redirectUrl;

}
