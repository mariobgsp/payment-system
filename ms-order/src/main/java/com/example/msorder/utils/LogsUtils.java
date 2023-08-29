package com.example.msorder.utils;

import java.util.UUID;

import com.example.msorder.model.logs.ServiceLog;
import com.example.msorder.model.rqrs.request.RequestInfo;
import com.example.msorder.model.rqrs.response.ResponseInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogsUtils {

    public static ServiceLog construcInsertLogs(RequestInfo requestInfo, ResponseInfo<Object> responseInfo) {
        ServiceLog serviceLog = new ServiceLog();

        try {
            serviceLog.setId(UUID.randomUUID().toString());
            serviceLog.setAppName(requestInfo.getAppName());
            serviceLog.setChannel(requestInfo.getChannel());
            serviceLog.setCompletionStatus(responseInfo.getBody().getStatus());
            serviceLog.setCorrelationId(requestInfo.getCorrelationId());
            serviceLog.setHttpStatusCode(responseInfo.getHttpStatus().value());
            serviceLog.setOperationName(requestInfo.getOpName());

            serviceLog.setRequestAt(CommonUtils.convertToDate(requestInfo.getRequestAt()));
            serviceLog.setRequestId(requestInfo.getRequestId());
            serviceLog.setRequestPayload(requestInfo.getRequestData());
            serviceLog.setResponsePayload(responseInfo.getBody());

            if (requestInfo.getHttpServletRequest() != null) {
                serviceLog.setUri(requestInfo.getHttpServletRequest().getRequestURI())
                        .setMethod(requestInfo.getHttpServletRequest().getMethod())
                        .setHost(requestInfo.getHttpServletRequest().getRemoteHost())
                        .setQueryParam(requestInfo.getHttpServletRequest().getQueryString());
            }
        } catch (Exception e) {
            log.error("Failed parse payload {}", e.getMessage());
        }

        return serviceLog;
    }

}
