package com.example.msinvoice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class OrderServices {

//    public PaymentRs paymentCharge(RequestInfo requestInfo, PaymentRq pamentRq) throws Exception {
//        PaymentRs paymentRs = new PaymentRs();y
//        String url = appProperties.getPAYMENT_CHARGE_URL();
//        RestTemplate restTemplate = new RestTemplate();
//        Map<String,Object> map = new HashMap<>();
//
//        log.info("[URL: {}]", url);
//        log.info("[Request: {}]", CommonUtils.gson.toJson(paymentRq));
//        ResponseEntity<String> response = restTemplate.exchange(
//                url,
//                HttpMethod.POST,
//                new HttpEntity<>(paymentRq,getHttpHeaders(requestInfo)), String.class);
//        if(response.getBody()==null){
//            log.error("[couldn't do payment charge]]");
//            throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "99", "failed to invoke");
//        }else{
//            if(response.getStatusCode().is2xxSuccessful()){
//                log.info("[Response: {}]",response.getBody());
//                paymentRs = CommonUtils.gson.fromJson(response.getBody(), PaymentRs.class);
//            }else{
//                throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "99", "failed to invoke");
//            }
//        }
//
//        return paymentRs;
//    }



}
