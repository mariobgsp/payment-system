package com.example.msorder.exception.definition;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends CommonException {

    public UserNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND , code , "UserNotFoundException" , message , message);
    }
}
