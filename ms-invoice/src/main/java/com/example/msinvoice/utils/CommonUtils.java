package com.example.msinvoice.utils;

import com.example.msinvoice.config.variable.ApplicationConstant;
import com.example.msinvoice.model.rqrs.request.RequestInfo;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
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

    public static Date convertToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static String convertDateToString(Date input){
        // Define the pattern for formatting
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        // Convert to formatted string
        return formatter.format(input);
    }

    public static String decimalToFormat(double input){

        // Set up symbols for German locale (for example)
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');

        // Define the pattern for formatting
        DecimalFormat formatter = new DecimalFormat("###,###.00", symbols);

        // Convert to formatted string
        return formatter.format(input);
    }

}
