package com.java.project.msorder.model.rqrs.response.order;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OrderRs {

    private String createdAt;
    private String TransactionId;

}
