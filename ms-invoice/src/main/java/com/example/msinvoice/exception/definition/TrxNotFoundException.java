package com.example.msinvoice.exception.definition;

import org.springframework.http.HttpStatus;

public class TrxNotFoundException extends CommonException {

    public TrxNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND , code , "TrxNotFoundException" , message , message);
    }
}
