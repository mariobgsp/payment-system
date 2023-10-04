package com.example.mspayment.model.mspayment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PaymentRefundRq {

    @JsonProperty("reference_id")
    private String referenceId;
    @JsonProperty("amount")
    private Long amount;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("currency")
    private String currency;


}
