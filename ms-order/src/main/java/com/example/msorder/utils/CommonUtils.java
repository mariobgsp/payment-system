package com.example.msorder.utils;

import com.example.msorder.config.variable.ApplicationConstant;
import com.example.msorder.model.rqrs.request.RequestInfo;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class CommonUtils {

    public static Gson gson = new Gson();

    public static RequestInfo constructRequestInfo(String channel, String opName, String requestId, Object payload, HttpServletRequest httpServletRequest){
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setAppName(ApplicationConstant.APPLICATION_NAME);
        requestInfo.setChannel(channel);
        requestInfo.setOpName(opName);
        requestInfo.setRequestId(requestId);
        requestInfo.setCorrelationId(ApplicationConstant.APP_PREFIX+UUID.randomUUID());
        requestInfo.setRequestData(payload);
        requestInfo.setHttpServletRequest(httpServletRequest);
        if(requestInfo.getRequestAt()==null){
            requestInfo.setRequestAt(dateFormatter(new Date()));
        }
        return requestInfo;
    }

    public static String dateFormatter(Date date){
        SimpleDateFormat formatter = new SimpleDateFormat(ApplicationConstant.DATE_TIME_FORMAT);
        TimeZone tz = TimeZone.getTimeZone("Asia/Jakarta");
        formatter.setTimeZone(tz);
        return formatter.format(date);
    }

    public static Date convertToDate(String sdate) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(ApplicationConstant.DATE_TIME_FORMAT);
        Date date = formatter.parse(sdate);

        return date;
    }

}
