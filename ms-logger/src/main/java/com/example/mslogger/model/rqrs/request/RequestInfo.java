package com.example.mslogger.model.rqrs.request;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestInfo {

    private String appName;
    private String requestId;
    private String channel;
    private String opName;
    private String correlationId;
    private String requestAt;
    private Object requestData;
    private HttpServletRequest httpServletRequest;

}
