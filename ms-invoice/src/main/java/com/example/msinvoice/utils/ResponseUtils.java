package com.example.msinvoice.utils;

import com.example.msinvoice.exception.definition.CommonException;
import com.example.msinvoice.model.rqrs.request.RequestInfo;
import com.example.msinvoice.model.rqrs.response.Response;
import com.example.msinvoice.model.rqrs.response.ResponseInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class ResponseUtils {

    public static ResponseInfo<Object> generateSuccessRs(RequestInfo requestInfo, Object body) {
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        Response<Object> response = new Response<>();
        response.setCode("00");
        response.setStatus("ok");
        response.setMessage("request-success");
        response.setData(body);

        responseInfo.setHttpStatus(HttpStatus.OK);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("x-request-id", requestInfo.getRequestId());
        httpHeaders.add("x-channel-id", requestInfo.getChannel());
        httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
        responseInfo.setHttpHeaders(httpHeaders);
        responseInfo.setBody(response);

        return responseInfo;
    }

    public static ResponseInfo<Object> generateSuccessRs(RequestInfo requestInfo) throws Exception {
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        Response<Object> response = new Response<>();
        response.setCode("00");
        response.setStatus("ok");
        response.setMessage("request-success");
        response.setData(null);

        responseInfo.setHttpStatus(HttpStatus.OK);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("x-request-id", requestInfo.getRequestId());
        httpHeaders.add("x-channel-id", requestInfo.getChannel());
        httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
        responseInfo.setHttpHeaders(httpHeaders);
        responseInfo.setBody(response);

        return responseInfo;
    }

    public static ResponseInfo<Object> generateMessageSuccessRs(RequestInfo requestInfo, String message) {
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        Response<Object> response = new Response<>();
        response.setCode("00");
        response.setStatus("ok");
        response.setMessage(message);
        response.setData(null);

        responseInfo.setHttpStatus(HttpStatus.OK);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("x-request-id", requestInfo.getRequestId());
        httpHeaders.add("x-channel-id", requestInfo.getChannel());
        httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
        responseInfo.setHttpHeaders(httpHeaders);
        responseInfo.setBody(response);

        return responseInfo;
    }


    public static ResponseInfo<Object> generateDefaultAsyncRs(RequestInfo requestInfo) {
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        Response<Object> response = new Response<>();
        response.setCode("00");
        response.setStatus("accepted");
        response.setMessage("Request being processed");
        response.setData(null);

        responseInfo.setHttpStatus(HttpStatus.ACCEPTED);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("x-request-id", requestInfo.getRequestId());
        httpHeaders.add("x-channel-id", requestInfo.getChannel());
        httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
        responseInfo.setHttpHeaders(httpHeaders);
        responseInfo.setBody(response);

        return responseInfo;
    }

    public static ResponseInfo<Object> generateException(RequestInfo requestInfo, CommonException e){
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();
        Response<Object> response = new Response<>();
        if(e.getHttpStatus()==null){
            response.setCode("99");
            response.setStatus("failed");
            response.setMessage(String.format("failed, cause:%s", e.getMessage()));
            response.setData(null);
            responseInfo.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }else{
            response.setCode(e.getCode());
            response.setStatus(e.getStatus());
            response.setMessage(String.format("failed, cause:%s", e.getMessage()));
            response.setData(null);
            responseInfo.setHttpStatus(e.getHttpStatus());
        }


        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("x-request-id", requestInfo.getRequestId());
        httpHeaders.add("x-channel-id", requestInfo.getChannel());
        httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
        responseInfo.setHttpHeaders(httpHeaders);
        responseInfo.setBody(response);

        return responseInfo;
    }

}
