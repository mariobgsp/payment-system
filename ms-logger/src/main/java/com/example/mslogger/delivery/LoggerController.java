package com.example.mslogger.delivery;

import com.example.mslogger.model.rqrs.request.RequestInfo;
import com.example.mslogger.model.rqrs.response.Response;
import com.example.mslogger.model.rqrs.response.ResponseInfo;
import com.example.mslogger.repository.MongoRepository;
import com.example.mslogger.utils.CommonUtils;

import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/ms/api")
public class LoggerController {

    @Autowired
    private MongoRepository mongoRepository;

    @GetMapping("/v0/logs")
    public ResponseEntity<?> insertLogs(HttpServletRequest httpServletRequest){

        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo("test-channel", "test-insert-log", UUID.randomUUID().toString(), "masterUser", httpServletRequest);

        // set response
        Response<Object> rs = new Response<>();
        rs.setCode("00");
        rs.setMessage("Success");
        rs.setData(null);
        rs.setStatus("ok");

        ResponseInfo<Object> response = new ResponseInfo<>();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("x-request-id", request.getRequestId());
        httpHeaders.add("x-channel-id", request.getChannel());
        httpHeaders.add("x-request-at", request.getRequestAt());
        response.setHttpHeaders(httpHeaders);
        response.setHttpStatus(HttpStatus.ACCEPTED);
        response.setBody(rs);

        mongoRepository.insertToMongodb(request, response);

        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }

}
