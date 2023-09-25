package com.example.mspayment.model.mspayment;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Actions {
    private String checkout_url;

}
