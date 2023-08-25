package com.example.msorder.exception.handler;

import org.springframework.http.HttpStatus;

import com.example.msorder.exception.definition.CommonException;

public class UserNotFoundException extends CommonException {

    public UserNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND , code , "UserNotFoundException" , message , message);
    }
}
