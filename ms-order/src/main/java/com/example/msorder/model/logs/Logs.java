package com.example.msorder.model.logs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Logs {

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
