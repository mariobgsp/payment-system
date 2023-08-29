package com.example.msorder.utils;

import java.util.UUID;

import com.example.msorder.model.logs.Logs;
import com.example.msorder.model.rqrs.request.RequestInfo;
import com.example.msorder.model.rqrs.response.ResponseInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogsUtils {

    public static Logs construcInsertLogs(RequestInfo requestInfo, ResponseInfo<Object> responseInfo) {
        Logs logs = new Logs();

        try {
            logs.setId(UUID.randomUUID().toString());
            logs.setAppName(requestInfo.getAppName());
            logs.setChannel(requestInfo.getChannel());
            logs.setCompletionStatus(responseInfo.getBody().getStatus());
            logs.setCorrelationId(requestInfo.getCorrelationId());
            logs.setHttpStatusCode(responseInfo.getHttpStatus().value());
            logs.setOperationName(requestInfo.getOpName());

            logs.setRequestAt(CommonUtils.convertToDate(requestInfo.getRequestAt()));
            logs.setRequestId(requestInfo.getRequestId());
            logs.setRequestPayload(CommonUtils.gson.toJson(requestInfo.getRequestData()));
            logs.setResponsePayload(CommonUtils.gson.toJson(responseInfo.getBody()));

            if (requestInfo.getHttpServletRequest() != null) {
                logs.setUri(requestInfo.getHttpServletRequest().getRequestURI())
                        .setMethod(requestInfo.getHttpServletRequest().getMethod())
                        .setHost(requestInfo.getHttpServletRequest().getRemoteHost())
                        .setQueryParam(requestInfo.getHttpServletRequest().getQueryString());
            }
        } catch (Exception e) {
            log.error("Failed parse payload {}", e.getMessage());
        }

        return logs;
    }

}
