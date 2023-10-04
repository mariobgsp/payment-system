package com.example.mspayment.model.rqrs.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Action {

    @JsonProperty("checkout_url")
    private String checkoutUrl;
}
