package com.java.project.msorder.utils;

import com.google.gson.Gson;
import com.java.project.msorder.model.rqrs.request.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
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
