package com.java.project.msorder.model.rqrs.response;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Data
public class ResponseInfo<T> {

    private HttpStatus httpStatus;
    private HttpHeaders httpHeaders;
    private Response<T> response;

}
