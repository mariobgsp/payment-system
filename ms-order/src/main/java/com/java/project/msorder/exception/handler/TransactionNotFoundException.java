package com.java.project.msorder.exception.handler;

import com.java.project.msorder.exception.definition.CommonException;
import org.springframework.http.HttpStatus;

public class TransactionNotFoundException extends CommonException {
    public TransactionNotFoundException(String code , String message) {
        super(HttpStatus.NOT_FOUND , code , "ProductNotFoundException" , message , message);
    }
}
