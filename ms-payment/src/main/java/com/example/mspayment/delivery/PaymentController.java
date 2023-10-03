package com.example.mspayment.delivery;

import com.example.mspayment.model.rqrs.request.CreatePaymentRq;
import com.example.mspayment.model.rqrs.request.RequestInfo;
import com.example.mspayment.model.rqrs.response.ResponseInfo;
import com.example.mspayment.usecase.PaymentUsecase;
import com.example.mspayment.utils.CommonUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ms/api")
public class PaymentController {

    @Autowired
    private PaymentUsecase paymentUsecase;

    @PostMapping("/v1/payment/create/{type}")
    public ResponseEntity<?> createPayment(@PathVariable("type") String paymentType,
                                           @RequestParam(value="transaction_id", required = true) String transactionId,
                                           @RequestParam(value="username") String userName,
                                           @RequestHeader(value="x-request-channel") String channel,
                                           @RequestHeader(value="x-request-id") String requestId,
                                           @RequestBody CreatePaymentRq bodyRq,
                                           HttpServletRequest httpServletRequest){
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "post-create-payment", requestId, userName, httpServletRequest);
        ResponseInfo<Object> response = paymentUsecase.createPayment(request, paymentType, transactionId, bodyRq);
        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }


}
