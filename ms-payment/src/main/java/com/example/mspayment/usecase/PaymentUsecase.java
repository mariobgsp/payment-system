package com.example.mspayment.usecase;

import com.example.mspayment.config.properties.AppProperties;
import com.example.mspayment.exception.definition.BadRequestException;
import com.example.mspayment.exception.definition.CommonException;
import com.example.mspayment.exception.definition.TrxNotFoundException;
import com.example.mspayment.model.mspayment.PaymentRefundRq;
import com.example.mspayment.model.mspayment.PaymentRefundRs;
import com.example.mspayment.model.mspayment.PaymentRq;
import com.example.mspayment.model.mspayment.PaymentRs;
import com.example.mspayment.model.repository.ProductTrx;
import com.example.mspayment.model.rqrs.request.*;
import com.example.mspayment.model.rqrs.response.CreatePaymentRs;
import com.example.mspayment.model.rqrs.response.ResponseInfo;
import com.example.mspayment.repository.TransactionRepository;
import com.example.mspayment.service.KafkaServices;
import com.example.mspayment.service.PaymentService;
import com.example.mspayment.utils.CommonUtils;
import com.example.mspayment.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PaymentUsecase extends BaseUsecase{

    @Autowired
    private AppProperties appProperties;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private KafkaServices kafkaServices;

    public ResponseInfo<Object> createPayment(RequestInfo requestInfo, String paymentType, String transactionId, CreatePaymentRq bodyRq){
        log.info("[{} - createPayment][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // find trx in product_trx
            List<ProductTrx> productTrxList = transactionRepository.findProductTrx(transactionId);
            if (productTrxList.isEmpty()){
                throw new TrxNotFoundException("01", "transaction not found");
            }

            // construct request rq
            PaymentRq paymentRq = new PaymentRq();
            paymentRq
                    .setAmount(productTrxList.get(0).getPriceCharge())
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
            log.error("[{} - createPayment][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }

        // publish log
        super.publish(requestInfo, responseInfo);
        return responseInfo;
    }

    public void refundPayment(RequestInfo requestInfo, RefundRq refundRq) {
        log.info("[{} - refundPayment][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());

        ResponseInfo<Object> responseInfo = new ResponseInfo<>();
        try{
            // find trx in product_trx
            List<ProductTrx> productTrxList = transactionRepository.findProductTrx(refundRq.getTransactionId());
            if (productTrxList.isEmpty()){
                throw new TrxNotFoundException("01", "transaction not found");
            }

            // invoke payment service
            PaymentRefundRq paymentRefundRq = new PaymentRefundRq()
                    .setReferenceId(productTrxList.get(0).getTransactionId())
                    .setCurrency(appProperties.getPAYMENT_CURRENCY())
                    .setReason("Request Refund")
                    .setAmount(productTrxList.get(0).getPriceCharge());

            PaymentRefundRs paymentRefundRs = paymentService.paymentRefund(requestInfo, paymentRefundRq);

            // update in multipayment trx
            transactionRepository.updateProductTrx(refundRq.getTransactionId() , productTrxList.get(0).getPaymentStatus() , appProperties.getPAYMENT_STATUS_REFUND());

            responseInfo = ResponseUtils.generateSuccessRs(requestInfo, paymentRefundRs);
        }catch (Exception e){
            log.error("[{} - createPayment][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        super.publish(requestInfo, responseInfo);
    }


    public void notifyPayment(RequestInfo requestInfo, CallbackRq bodyRq){
        log.info("[{} - notifyPayment][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try {
            // find trx in product_trx
            List<ProductTrx> productTrxList = transactionRepository.findProductTrx(bodyRq.getReferenceId());
            if (productTrxList.isEmpty()){
                throw new TrxNotFoundException("01", "transaction not found");
            }

            if(bodyRq.getStatus().equals(appProperties.getPAYMENT_SUCCEEDED())){
                // update in multipayment trx
                transactionRepository.updateProductTrx(bodyRq.getReferenceId() , appProperties.getORDER_STATUS_PUBLISHED() , appProperties.getPAYMENT_STATUS_SUCCESS());

                // construct kafka message
                KafkaMessageRq kafkaMessageRq = new KafkaMessageRq();
                productTrxList = transactionRepository.findProductTrx(bodyRq.getReferenceId());

                kafkaMessageRq.setId(productTrxList.get(0).getId())
                        .setSysCreationDate(productTrxList.get(0).getSysCreationDate())
                        .setTransactionId(productTrxList.get(0).getTransactionId())
                        .setOrderStatus(productTrxList.get(0).getOrderStatus())
                        .setPaymentStatus(productTrxList.get(0).getPaymentStatus())
                        .setUserId(productTrxList.get(0).getUserId())
                        .setProductName(productTrxList.get(0).getProductName())
                        .setAmount(productTrxList.get(0).getAmount())
                        .setPrice(productTrxList.get(0).getPrice())
                        .setPriceCharge(productTrxList.get(0).getPriceCharge())
                        .setProductCode(productTrxList.get(0).getProductCode())
                        .setSysUpdateDate(productTrxList.get(0).getSysUpdateDate());
                // publish kafka
                kafkaServices.publishPayment("ms-notify-payment", CommonUtils.gson.toJson(kafkaMessageRq));
            }else {
                log.info("Notify Payment received, trxid : {}, status {}", bodyRq.getReferenceId(), bodyRq.getStatus());
            }

            responseInfo = ResponseUtils.generateSuccessRs(requestInfo);
        }catch (Exception e){
            log.error("[{} - notifyPayment][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }

        // publish log
        super.publish(requestInfo, responseInfo);
    }

}
