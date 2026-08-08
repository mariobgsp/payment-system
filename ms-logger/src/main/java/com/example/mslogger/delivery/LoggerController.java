package com.example.mslogger.delivery;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.mslogger.model.rqrs.request.RequestInfo;
import com.example.mslogger.model.rqrs.response.ResponseInfo;
import com.example.mslogger.utils.CommonUtils;
import com.example.mslogger.utils.ResponseUtils;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ms/api")
@Slf4j
public class LoggerController {

    @GetMapping("/v1/health/check")
    public ResponseEntity<?> checkLoggerHealth(){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo("health-check-ms-logger", "health-check-ms-logger", UUID.randomUUID().toString(), "request-publish-log", null);

        // set response
        ResponseInfo<Object> response = ResponseUtils.generateMessageSuccessRs(request, "success-check-health");
        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());

    }



}
