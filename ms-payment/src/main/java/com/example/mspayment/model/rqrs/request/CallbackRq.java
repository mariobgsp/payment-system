package com.example.mspayment.model.rqrs.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CallbackRq {

    // 	Id                string              `json:"id"`
    //	Status            string              `json:"status"`
    //	Currency          string              `json:"currency"`
    //	CheckoutMethod    string              `json:"checkout_method"`
    //	Amount            int                 `json:"amount"`
    //	PaymentCode       string              `json:"payment_code"`
    //	ReferenceId       string              `json:"reference_id"`
    //	ChannelProperties *ChannelProperties  `json:"channel_properties"`
    //	CallbackUrl       string              `json:"callback_url"`
    //	Created           time.Time           `json:"created"`
    //	Updated           time.Time           `json:"updated"`
    //	Action            *Action             `json:"action"`
    //	Internal          *AdditionalResponse `json:"-"`

    @JsonProperty("id")
    private String id;
    @JsonProperty("status")
    private String status;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("checkout_method")
    private String checkoutMethod;
    @JsonProperty("amount")
    private int amount;
    @JsonProperty("payment_code")
    private String paymentCode;
    @JsonProperty("reference_id")
    private String ReferenceId;
    @JsonProperty("channel_properties")
    private String channelProperties;
    @JsonProperty("callback_url")
    private String callbackUrl;
    @JsonProperty("created")
    private String created;
    @JsonProperty("updated")
    private String Updated;
    @JsonProperty("action")
    private Action action;



}
