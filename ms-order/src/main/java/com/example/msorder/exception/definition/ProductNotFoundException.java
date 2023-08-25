package com.example.msorder.exception.handler;

import org.springframework.http.HttpStatus;

import com.example.msorder.exception.definition.CommonException;

public class ProductNotFoundException extends CommonException {
    public ProductNotFoundException(String code , String message) {
        super(HttpStatus.NOT_FOUND , code , "ProductNotFoundException" , message , message);
    }
}
