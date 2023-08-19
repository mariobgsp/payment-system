package com.java.project.msorder.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.java.project.msorder.exception.definition.CommonException;
import com.java.project.msorder.exception.model.ApiFault;
import com.java.project.msorder.model.rqrs.request.RequestInfo;
import com.java.project.msorder.model.rqrs.response.Response;
import com.java.project.msorder.model.rqrs.response.ResponseInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class CommonUtils {

    public static Gson gson = new Gson();
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static RequestInfo constructRequestInfo(String channel, String opName, String requestId, Object payload, HttpServletRequest httpServletRequest){
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setChannel(channel);
        requestInfo.setOpName(opName);
        requestInfo.setRequestId(requestId);
        requestInfo.setRequestData(payload);
        requestInfo.setHttpServletRequest(httpServletRequest);
        if(requestInfo.getRequestAt()==null){
            requestInfo.setRequestAt(dateFormatter(new Date()));
        }
        return requestInfo;
    }

    public static String dateFormatter(Date date){
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT);
        TimeZone tz = TimeZone.getTimeZone("Asia/Jakarta");
        formatter.setTimeZone(tz);
        return formatter.format(date);
    }

}
