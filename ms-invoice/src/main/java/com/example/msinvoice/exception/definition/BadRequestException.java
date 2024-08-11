package com.example.msinvoice.exception.definition;

import org.springframework.http.HttpStatus;

public class BadRequestException extends CommonException {
    public BadRequestException(String code , String message) {
        super(HttpStatus.BAD_REQUEST , code , "BadRequestException" , message , message);
    }
}
