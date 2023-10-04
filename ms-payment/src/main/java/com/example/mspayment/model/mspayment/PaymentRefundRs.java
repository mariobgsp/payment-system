package com.example.mspayment.model.mspayment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PaymentRefundRs {

    @JsonProperty("id")
    private String id;
    @JsonProperty("reference_id")
    private String referenceId;
    @JsonProperty("amount")
    private String amount;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("refund_status")
    private String refundStatus;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;

}
