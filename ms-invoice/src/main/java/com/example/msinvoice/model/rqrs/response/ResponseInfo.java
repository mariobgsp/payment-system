package com.example.msinvoice.model.rqrs.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Data
public class ResponseInfo<T> {
    @JsonIgnore
    private HttpStatus httpStatus;
    @JsonIgnore
    private HttpHeaders httpHeaders;
    private Response<T> body;
}
