package com.example.msorder.exception.handler;

import org.springframework.http.HttpStatus;

import com.example.msorder.exception.definition.CommonException;

public class TransactionNotFoundException extends CommonException {
    public TransactionNotFoundException(String code , String message) {
        super(HttpStatus.NOT_FOUND , code , "TransactionNotFoundException" , message , message);
    }
}
