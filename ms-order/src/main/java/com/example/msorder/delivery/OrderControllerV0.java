package com.example.msorder.delivery;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.msorder.model.rqrs.request.RequestInfo;
import com.example.msorder.model.rqrs.response.ResponseInfo;
import com.example.msorder.usecase.SimpleUsecase;
import com.example.msorder.utils.CommonUtils;

@RestController
@RequestMapping("/ms/api")
public class OrderControllerV0 {

    @Autowired
    private SimpleUsecase simpleUsecase;

    @GetMapping("/v0/get-all-product")
    public ResponseEntity<?> getAllProduct(
            @RequestParam(value="username",required = false) String userName,
            @RequestHeader(value="x-request-channel") String channel,
            @RequestHeader(value="x-request-id") String requestId,
            HttpServletRequest httpServletRequest){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "get-all-product", requestId, userName, httpServletRequest);
        ResponseInfo<Object> response = simpleUsecase.getAllProduct(request);
        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }

    @GetMapping("/v0/get-available-product")
    public ResponseEntity<?> getAvailableProduct(
            @RequestParam(value="username",required = false) String userName,
            @RequestHeader(value="x-request-channel") String channel,
            @RequestHeader(value="x-request-id") String requestId,
            HttpServletRequest httpServletRequest){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "get-available-product", requestId, userName, httpServletRequest);
        ResponseInfo<Object> response = simpleUsecase.getAvailableProduct(request);
        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }


}
