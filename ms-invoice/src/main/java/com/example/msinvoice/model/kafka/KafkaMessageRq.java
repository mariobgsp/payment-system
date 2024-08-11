package com.example.msinvoice.model.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class KafkaMessageRq {

    @JsonProperty("id")
    @SerializedName("id")
    private String id;
    @JsonProperty("sysCreationDate")
    @SerializedName("sysCreationDate")
    private String sysCreationDate;
    @JsonProperty("transactionId")
    @SerializedName("transactionId")
    private String transactionId;
    @JsonProperty("orderStatus")
    @SerializedName("orderStatus")
    private String orderStatus;
    @JsonProperty("paymentStatus")
    @SerializedName("paymentStatus")
    private String paymentStatus;
    @JsonProperty("userId")
    @SerializedName("userId")
    private String userId;
    @JsonProperty("productName")
    @SerializedName("productName")
    private String productName;
    @JsonProperty("amount")
    @SerializedName("amount")
    private Long amount;
    @JsonProperty("price")
    @SerializedName("price")
    private Long price;
    @JsonProperty("priceCharge")
    @SerializedName("priceCharge")
    private Long priceCharge;
    @JsonProperty("productCode")
    @SerializedName("productCode")
    private String productCode;
    @JsonProperty("sysUpdateDate")
    @SerializedName("sysUpdateDate")
    private String sysUpdateDate;

}
