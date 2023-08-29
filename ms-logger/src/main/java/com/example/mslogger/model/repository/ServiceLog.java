package com.example.mslogger.model.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ServiceLog {

    @JsonProperty("id")
    @SerializedName("id")
    private String id;

    @JsonProperty("appName")
    @SerializedName("appName")
    private String appName;

    @JsonProperty("channel")
    @SerializedName("channel")
    private String channel;

    @JsonProperty("completionStatus")
    @SerializedName("completionStatus")
    private String completionStatus;

    @JsonProperty("correlationId")
    @SerializedName("correlationId")
    private String correlationId;

    @JsonProperty("method")
    @SerializedName("method")
    private String method;

    @JsonProperty("operationName")
    @SerializedName("operationName")
    private String operationName;

    @JsonProperty("requestAt")
    @SerializedName("requestAt")
    private Date requestAt;

    @JsonProperty("requestId")
    @SerializedName("requestId")
    private String requestId;

    @JsonProperty("host")
    @SerializedName("host")
    private String host;

    @JsonProperty("httpStatusCode")
    @SerializedName("httpStatusCode")
    private int httpStatusCode;

    @JsonProperty("uri")
    @SerializedName("uri")
    private String uri;

    @JsonProperty("queryParam")
    @SerializedName("queryParam")
    private String queryParam;

    @JsonProperty("requestHeaders")
    @SerializedName("requestHeaders")
    private Map<String, String> requestHeaders;

    @JsonProperty("requestPayload")
    @SerializedName("requestPayload")
    private Object requestPayload;

    @JsonProperty("responsePayload")
    @SerializedName("responsePayload")
    private Object responsePayload;


}
