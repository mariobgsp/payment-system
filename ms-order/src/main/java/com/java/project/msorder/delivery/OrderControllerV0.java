package com.java.project.msorder.delivery;

import com.java.project.msorder.model.rqrs.request.RequestInfo;
import com.java.project.msorder.model.rqrs.response.ResponseInfo;
import com.java.project.msorder.usecase.OrderUsecase;
import com.java.project.msorder.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ms/api")
public class OrderControllerV0 {

    @Autowired
    private OrderUsecase orderUsecase;

    @GetMapping("/v0/get-all-product")
    public ResponseEntity<?> getAllProduct(
            @RequestParam(value="user_name",required = false) String userName,
            @RequestHeader(value="x-request-channel") String channel,
            @RequestHeader(value="x-request-id") String requestId){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "get-all-product", requestId, userName);
        ResponseInfo<Object> response = orderUsecase.getAllProduct(request);
        return new ResponseEntity<>(response, response.getHttpHeaders(), response.getHttpStatus());
    }

    @GetMapping("/v0/get-available-product")
    public ResponseEntity<?> getAvailableProduct(
            @RequestParam(value="user_name",required = false) String userName,
            @RequestHeader(value="x-request-channel") String channel,
            @RequestHeader(value="x-request-id") String requestId){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "get-available-product", requestId, userName);
        ResponseInfo<Object> response = orderUsecase.getAvailableProduct(request);
        return new ResponseEntity<>(response, response.getHttpHeaders(), response.getHttpStatus());
    }

    @GetMapping("/v0/{username}/user-detail")
    public ResponseEntity<?> getUserDetail(
            @PathVariable("username") String userName,
            @RequestHeader(value="x-request-channel") String channel,
            @RequestHeader(value="x-request-id") String requestId){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "get-user-detail", requestId, userName);
        ResponseInfo<Object> response = orderUsecase.getUserInfo(request, userName);
        return new ResponseEntity<>(response, response.getHttpHeaders(), response.getHttpStatus());
    }



}
