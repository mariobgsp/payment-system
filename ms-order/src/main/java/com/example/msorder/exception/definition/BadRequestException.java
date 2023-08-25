package com.example.msorder.exception.definition;

import org.springframework.http.HttpStatus;

import com.example.msorder.exception.definition.CommonException;

public class BadRequestException extends CommonException {
    public BadRequestException(String code , String message) {
        super(HttpStatus.BAD_REQUEST , code , "BadRequestException" , message , message);
    }
}
