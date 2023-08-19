package com.java.project.msorder.model.rqrs.response;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ViewProductRs {

    private String productCode;
    private String productName;
    private Integer price;
    private double discount;
    private boolean discountAvailable;
    private String productUpdateDate;
    private String productInsertDate;

}
