package com.example.mspayment.service;

import com.example.mspayment.config.properties.AppProperties;
import com.example.mspayment.exception.definition.PaymentException;
import com.example.mspayment.model.mspayment.PaymentRefundRq;
import com.example.mspayment.model.mspayment.PaymentRefundRs;
import com.example.mspayment.model.mspayment.PaymentRq;
import com.example.mspayment.model.mspayment.PaymentRs;
import com.example.mspayment.model.rqrs.request.RequestInfo;
import com.example.mspayment.utils.CommonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PaymentService {

    @Autowired
    private AppProperties appProperties;

    public HttpHeaders getHttpHeaders(RequestInfo requestInfo){
        HttpHeaders headers = new HttpHeaders();
        headers.add("request-id", requestInfo.getRequestId());
        headers.add("api-key", "test");
        return headers;
    }

    public PaymentRs paymentCharge(RequestInfo requestInfo, PaymentRq paymentRq) throws Exception {
        PaymentRs paymentRs = new PaymentRs();
        String url = appProperties.getPAYMENT_CHARGE_URL();
        RestTemplate restTemplate = new RestTemplate();
        Map<String,Object> map = new HashMap<>();

        log.info("[URL: {}]", url);
        log.info("[Request: {}]", CommonUtils.gson.toJson(paymentRq));
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(paymentRq,getHttpHeaders(requestInfo)), String.class);
        if(response.getBody()==null){
            log.error("[couldn't do payment charge]]");
            throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "99", "failed to invoke");
        }else{
            if(response.getStatusCode().is2xxSuccessful()){
                log.info("[Response: {}]",response.getBody());
                paymentRs = CommonUtils.gson.fromJson(response.getBody(), PaymentRs.class);
            }else{
                throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "99", "failed to invoke");
            }
        }

        return paymentRs;
    }

    public PaymentRefundRs paymentRefund(RequestInfo requestInfo, PaymentRefundRq refundRq) throws Exception {
        PaymentRefundRs paymentRefundRs = new PaymentRefundRs();
        String url = appProperties.getPAYMENT_REFUND_URL();
        RestTemplate restTemplate = new RestTemplate();
        Map<String,Object> map = new HashMap<>();
        log.info("[URL: {}]", url);
        log.info("[Request: {}]", CommonUtils.gson.toJson(refundRq));
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(refundRq,getHttpHeaders(requestInfo)), String.class);
        log.info("[Response: {}]", response);
        if(response.getBody()==null){
            log.error("[couldn't do payment charge]]");
            throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "99", "failed to invoke");
        }else{
            if(response.getStatusCode().is2xxSuccessful()){
                log.info("{}",response.getBody());
                paymentRefundRs = CommonUtils.gson.fromJson(response.getBody(), PaymentRefundRs.class);
            }else{
                throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "99", "failed to invoke");
            }
        }

        return paymentRefundRs;
    }


}
