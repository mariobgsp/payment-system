package com.example.mspayment.delivery;

import com.example.mspayment.model.rqrs.request.CallbackRq;
import com.example.mspayment.model.rqrs.request.CreatePaymentRq;
import com.example.mspayment.model.rqrs.request.RequestInfo;
import com.example.mspayment.model.rqrs.response.ResponseInfo;
import com.example.mspayment.usecase.PaymentUsecase;
import com.example.mspayment.utils.CommonUtils;
import com.example.mspayment.utils.ResponseUtils;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/ms/api")
public class NotifyController {

    @Autowired
    private PaymentUsecase paymentUsecase;

    @PostMapping("/v1/payment/notify")
    public ResponseEntity<?> createPayment(@RequestHeader(value="x-request-channel", defaultValue = "PAYMENT-AGGR", required = false) String channel,
                                           @RequestHeader(value="x-request-id", required = false) String requestId,
                                           @RequestBody CallbackRq bodyRq,
                                           HttpServletRequest httpServletRequest){
        if (StringUtils.isEmpty(requestId)){
            requestId = UUID.randomUUID().toString();
        }
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "post-notify-payment", requestId, bodyRq, httpServletRequest);
        // run async
        CompletableFuture.runAsync(()-> paymentUsecase.notifyPayment(request, bodyRq));
        // construct response
        ResponseInfo<Object> response = ResponseUtils.generateDefaultAsyncRs(request);
        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }
}
