package com.example.mspayment.delivery;

import com.example.mspayment.config.properties.AppProperties;
import com.example.mspayment.exception.definition.CommonException;
import com.example.mspayment.model.rqrs.request.CallbackRq;
import com.example.mspayment.model.rqrs.request.RequestInfo;
import com.example.mspayment.model.rqrs.response.ResponseInfo;
import com.example.mspayment.usecase.PaymentUsecase;
import com.example.mspayment.utils.CommonUtils;
import com.example.mspayment.utils.ResponseUtils;
import com.example.mspayment.utils.SecurityUtils;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/ms/api")
@Slf4j
public class NotifyController {

    @Autowired
    private PaymentUsecase paymentUsecase;
    @Autowired
    private AppProperties appProperties;

    @PostMapping("/v1/payment/notify")
    public ResponseEntity<?> createPayment(@RequestHeader(value="x-request-channel", defaultValue = "PAYMENT-AGGR", required = false) String channel,
                                           @RequestHeader(value="x-request-id", required = false) String requestId,
                                           @RequestBody String body,
                                           HttpServletRequest httpServletRequest){
        if (StringUtils.isEmpty(requestId)){
            requestId = UUID.randomUUID().toString();
        }
        // construct request info
        RequestInfo request = CommonUtils.constructRequestInfo(channel, "post-notify-payment", requestId, body, httpServletRequest);

        // verify signature before accepting the callback
        String signature = httpServletRequest.getHeader("x-callback-signature");
        String timestamp = httpServletRequest.getHeader("x-callback-timestamp");
        if (!SecurityUtils.verifyCallbackSignature(appProperties.getNotifySecret(), timestamp, body, signature)) {
            log.warn("[{}] rejected callback with invalid or missing signature", requestId);
            CommonException ex = new CommonException(HttpStatus.UNAUTHORIZED, "41", "SecurityException", "unauthorized", "invalid callback signature");
            ResponseInfo<Object> response = ResponseUtils.generateException(request, ex);
            return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
        }

        CallbackRq bodyRq = CommonUtils.gson.fromJson(body, CallbackRq.class);
        // run async
        CompletableFuture.runAsync(()-> paymentUsecase.notifyPayment(request, bodyRq));
        // construct response
        ResponseInfo<Object> response = ResponseUtils.generateDefaultAsyncRs(request);
        return new ResponseEntity<>(response.getBody(), response.getHttpHeaders(), response.getHttpStatus());
    }
}
