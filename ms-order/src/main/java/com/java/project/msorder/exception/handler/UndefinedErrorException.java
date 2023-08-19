package com.java.project.msorder.exception.handler;

import com.java.project.msorder.exception.definition.CommonException;

public class UndefinedErrorException extends CommonException {
    public UndefinedErrorException(Exception e) {
        super(e);
    }
}
