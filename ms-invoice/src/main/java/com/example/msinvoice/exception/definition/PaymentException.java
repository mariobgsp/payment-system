package com.example.msinvoice.exception.definition;

import org.springframework.http.HttpStatus;

public class PaymentException extends CommonException{
    public PaymentException(HttpStatus httpStatus , String code , String message) {
        super(httpStatus , code , "PaymentException" , message , message);
    }

    public PaymentException(Exception e) {
        super(e);
    }
}
