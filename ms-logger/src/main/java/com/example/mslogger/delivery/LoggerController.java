package com.example.mslogger.delivery;

import java.util.UUID;

import com.example.mslogger.model.repository.ServiceLog;
import com.example.mslogger.service.KafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.mslogger.model.rqrs.request.RequestInfo;
import com.example.mslogger.model.rqrs.response.ResponseInfo;
import com.example.mslogger.utils.CommonUtils;
import com.example.mslogger.utils.LogsUtils;
import com.example.mslogger.utils.ResponseUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ms/api")
@Slf4j
public class LoggerController {

    @Autowired
    private KafkaService loggerPublisher;

    @PostMapping("/v0/publish")
    public ResponseEntity<?> publishLogs(HttpServletRequest httpServletRequest){

        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo("test-channel", "test-insert-log", UUID.randomUUID().toString(), "request-publish-log", httpServletRequest);

        // set response
        ResponseInfo<Object> response = ResponseUtils.generateMessageSuccessRs(request, "success-publishing-message");

        // construct payload
        ServiceLog insertServiceLog = LogsUtils.construcInsertLogs(request, response);

        // publish to topic
        try{
            loggerPublisher.publish(CommonUtils.gson.toJson(insertServiceLog));
        }catch(Exception e){
            log.error("error publish message {}", e.getMessage());
        }

        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }


}
