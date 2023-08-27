package com.example.msorder.exception.definition;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends CommonException {
    public ProductNotFoundException(String code , String message) {
        super(HttpStatus.NOT_FOUND , code , "ProductNotFoundException" , message , message);
    }
}
