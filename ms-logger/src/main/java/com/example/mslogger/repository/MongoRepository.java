package com.example.mslogger.repository;

import com.example.mslogger.config.variable.ApplicationConstant;
import com.example.mslogger.model.repository.InsertLogs;
import com.example.mslogger.model.rqrs.request.RequestInfo;
import com.example.mslogger.model.rqrs.response.ResponseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Repository
public class MongoRepository {

    @Autowired
    @Qualifier("mongo-template")
    private MongoTemplate mongoTemplate;
    public void insertToMongodb(RequestInfo requestInfo, ResponseInfo<Object> responseInfo){
        log.info("insert logs: {}, {}", requestInfo.getRequestId(), requestInfo.getOpName());
        try{
            InsertLogs insertLogs = new InsertLogs();
            insertLogs.setId(UUID.randomUUID().toString());
            insertLogs.setAppName(requestInfo.getAppName());
            insertLogs.setChannel(requestInfo.getChannel());
            insertLogs.setCompletionStatus(responseInfo.getBody().getStatus());
            insertLogs.setCorrelationId(requestInfo.getCorrelationId());
            insertLogs.setHttpStatusCode(responseInfo.getHttpStatus().value());
            insertLogs.setOperationName(requestInfo.getOpName());

            insertLogs.setRequestAt(convertToDate(requestInfo.getRequestAt()));
            insertLogs.setRequestId(requestInfo.getRequestId());
            insertLogs.setRequestPayload(requestInfo.getRequestData());
            insertLogs.setResponsePayload(responseInfo.getBody());

            if(requestInfo.getHttpServletRequest()!=null){
                insertLogs.setUri(requestInfo.getHttpServletRequest().getRequestURI())
                        .setMethod(requestInfo.getHttpServletRequest().getMethod())
                        .setHost(requestInfo.getHttpServletRequest().getRemoteHost())
                        .setQueryParam(requestInfo.getHttpServletRequest().getQueryString());
            }

            // insert
            mongoTemplate.insert(insertLogs);
            log.info("success insert logs, {}, {}",  requestInfo.getRequestId(), requestInfo.getOpName());
        }catch (Exception e){
            log.error("error insert to mongodb {}", e.getMessage());
        }

    }

    public Date convertToDate(String sdate) throws ParseException {
        return new SimpleDateFormat(ApplicationConstant.DATE_TIME_FORMAT).parse(sdate);
    }

}
