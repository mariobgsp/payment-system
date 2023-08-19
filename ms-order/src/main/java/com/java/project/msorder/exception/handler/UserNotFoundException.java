package com.java.project.msorder.exception.handler;

import com.java.project.msorder.exception.definition.CommonException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends CommonException {

    public UserNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND , code , "UserNotFoundException" , message , message);
    }
}
