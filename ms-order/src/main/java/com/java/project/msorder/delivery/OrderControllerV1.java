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
@Slf4j
public class OrderControllerV1 {

    @Autowired
    private OrderUsecase orderUsecase;

    @GetMapping("/v1/product")
    public ResponseEntity<?> viewProduct(
            @RequestParam(value="user_name",required = false) String userName,
            @RequestHeader(value="x-request-channel") String channel,
            @RequestHeader(value="x-request-id") String requestId){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "view-product", requestId, userName);
        log.info("[{}][REQUEST RECEIVED][{}][by: {}]",requestId, request.getOpName(), userName);
        ResponseInfo<Object> response = orderUsecase.viewProduct(request, userName);
        log.info("[{}][REQUEST COMPLETED][{}][response: {}]",requestId, request.getOpName(), response);
        return new ResponseEntity<>(response, response.getHttpHeaders(), response.getHttpStatus());
    }
}
