package com.example.mspayment.usecase;

import com.example.mspayment.config.properties.AppProperties;
import com.example.mspayment.exception.definition.CommonException;
import com.example.mspayment.model.mspayment.PaymentRq;
import com.example.mspayment.model.mspayment.PaymentRs;
import com.example.mspayment.model.repository.ProductTrx;
import com.example.mspayment.model.rqrs.request.CreatePaymentRq;
import com.example.mspayment.model.rqrs.request.RequestInfo;
import com.example.mspayment.model.rqrs.response.CreatePaymentRs;
import com.example.mspayment.model.rqrs.response.ResponseInfo;
import com.example.mspayment.repository.TransactionRepository;
import com.example.mspayment.service.PaymentService;
import com.example.mspayment.utils.ResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PaymentUsecase extends BaseUsecase{

    @Autowired
    private AppProperties appProperties;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private PaymentService paymentService;

    public ResponseInfo<Object> createPayment(RequestInfo requestInfo, String paymentType, String transactionId, CreatePaymentRq bodyRq){
        log.info("[{} - viewProduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // find trx in product_trx
            List<ProductTrx> productTrxList = transactionRepository.findProductTrx(transactionId);

            // construct request rq
            PaymentRq paymentRq = new PaymentRq();
            paymentRq
                    .setAmount(productTrxList.get(0).getAmount())
                    .setCurrency(appProperties.getPAYMENT_CURRENCY())
                    .setPaymentCode(paymentType)
                    .setReferenceId(transactionId)
                    .setCheckoutMethod(appProperties.getPAYMENT_CHECKOUT_METHOD())
                    .setRedirectUrl(bodyRq.getCallbackUrl());

            // invoke payment service
            PaymentRs paymentRs = paymentService.paymentCharge(requestInfo, paymentRq);

            // update into paymentstatus: READY, orderstatus READY
            transactionRepository.updateProductTrx(transactionId, appProperties.getORDER_STATUS_READY() , appProperties.getPAYMENT_STATUS_READY());

            CreatePaymentRs createPaymentRs = new CreatePaymentRs();
            if(paymentRs.getAction().getCheckout_url()!=null){
                createPaymentRs.setCheckoutUrl(paymentRs.getAction().getCheckout_url());
            }
            responseInfo = ResponseUtils.generateSuccessRs(requestInfo, createPaymentRs);
        }catch (Exception e) {
            log.error("[{} - getAllproduct][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }

        // publish log
        super.publish(requestInfo, responseInfo);
        return responseInfo;
    }



}
