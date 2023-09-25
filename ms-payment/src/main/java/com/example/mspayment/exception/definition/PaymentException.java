package com.example.mspayment.exception.definition;

import org.springframework.http.HttpStatus;

public class PaymentException extends CommonException{
    public PaymentException(HttpStatus httpStatus , String code , String message) {
        super(httpStatus , code , "PaymentException" , message , message);
    }

    public PaymentException(Exception e) {
        super(e);
    }
}
