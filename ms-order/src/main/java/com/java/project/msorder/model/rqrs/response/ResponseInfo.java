package com.java.project.msorder.model.rqrs.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.java.project.msorder.exception.model.ApiFault;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.List;

@Data
public class ResponseInfo<T> {
    @JsonIgnore
    private HttpStatus httpStatus;
    @JsonIgnore
    private HttpHeaders httpHeaders;
    private Response<T> body;
}
