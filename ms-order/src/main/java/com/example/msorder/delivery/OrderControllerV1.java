package com.example.msorder.delivery;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.msorder.model.rqrs.request.RequestInfo;
import com.example.msorder.model.rqrs.request.order.OrderRq;
import com.example.msorder.model.rqrs.response.ResponseInfo;
import com.example.msorder.usecase.OrderUsecase;
import com.example.msorder.utils.CommonUtils;

@RestController
@RequestMapping("/ms/api")
@Slf4j
public class OrderControllerV1 {

    @Autowired
    private OrderUsecase orderUsecase;

    @GetMapping("/v1/{username}/user-detail")
    public ResponseEntity<?> getUserDetail(
            @PathVariable("username") String userName,
            @RequestHeader(value="x-request-channel") String channel,
            @RequestHeader(value="x-request-id") String requestId,
            HttpServletRequest httpServletRequest){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "get-user-detail", requestId, userName, httpServletRequest);
        ResponseInfo<Object> response = orderUsecase.getUserInfo(request, userName);
        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }

    @GetMapping("/v1/view/product")
    public ResponseEntity<?> viewProduct(
            @RequestParam(value="user_name",required = false) String userName,
            @RequestHeader(value="x-request-channel") String channel,
            @RequestHeader(value="x-request-id") String requestId,
            HttpServletRequest httpServletRequest){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "view-product", requestId, userName, httpServletRequest);
        log.info("[{}][REQUEST RECEIVED][{}][by: {}]",requestId, request.getOpName(), userName);
        ResponseInfo<Object> response = orderUsecase.viewProduct(request, userName);
        log.info("[{}][REQUEST COMPLETED][{}][response: {}]",requestId, request.getOpName(), response);
        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }

    @PostMapping("/v1/order/product")
    public ResponseEntity<?> orderProduct(
            @RequestParam(value="user_name", required = false) String username,
            @RequestHeader(value="x-request-channel") String channel,
            @RequestHeader(value="x-request-id") String requestId,
            @RequestBody OrderRq bodyRq,
            HttpServletRequest httpServletRequest
    ){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "order-product", requestId, bodyRq, httpServletRequest);
        log.info("[{}][REQUEST RECEIVED][{}][by: {}]",requestId, request.getOpName(), bodyRq);
        ResponseInfo<Object> response = orderUsecase.orderProduct(request, username, bodyRq);
        log.info("[{}][REQUEST COMPLETED][{}][response: {}]",requestId, request.getOpName(), response);

        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }

    @GetMapping("/v1/order/{transactionid}/check")
    public ResponseEntity<?> getOrderStatus(
            @PathVariable("transactionid") String transactionId,
            @RequestParam(value="user_name", required = false) String username,
            @RequestHeader(value="x-request-channel") String channel,
            @RequestHeader(value="x-request-id") String requestId,
            HttpServletRequest httpServletRequest
    ){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "get-transaction-status", requestId, transactionId, httpServletRequest);
        log.info("[{}][REQUEST RECEIVED][{}][by: {}]",requestId, request.getOpName(), transactionId);
        ResponseInfo<Object> response = orderUsecase.checkProduct(request, username, transactionId);
        log.info("[{}][REQUEST COMPLETED][{}][response: {}]",requestId, request.getOpName(), response);

        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }


}
