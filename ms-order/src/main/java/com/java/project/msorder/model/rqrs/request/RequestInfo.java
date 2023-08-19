package com.java.project.msorder.model.rqrs.request;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestInfo {

    private String requestId;
    private String channel;
    private String opName;
    private String requestAt;
    private Object requestData;
    private HttpServletRequest httpServletRequest;

}
