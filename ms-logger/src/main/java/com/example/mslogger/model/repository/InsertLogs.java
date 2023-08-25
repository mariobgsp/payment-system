package com.example.mslogger.model.repository;

import com.example.mslogger.model.rqrs.request.RequestInfo;
import com.example.mslogger.model.rqrs.response.ResponseInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.experimental.Accessors;

import java.net.http.HttpHeaders;
import java.util.Date;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class InsertLogs {

    private String id;
    private String appName;
    private String channel;
    private String completionStatus;
    private String correlationId;
    private String method;

    private String operationName;

    private Date requestAt;
    private String requestId;

    private String host;
    private int httpStatusCode;
    private String uri;
    private String queryParam;

    private Map<String, String> requestHeaders;
    private Object requestPayload;
    private Object responsePayload;


}
