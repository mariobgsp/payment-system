package com.example.mspayment.usecase;

import com.example.mspayment.config.properties.AppProperties;
import com.example.mspayment.model.mspayment.PaymentRs;
import com.example.mspayment.model.mspayment.Actions;
import com.example.mspayment.model.repository.ProductTrx;
import com.example.mspayment.model.rqrs.request.CreatePaymentRq;
import com.example.mspayment.model.rqrs.request.RequestInfo;
import com.example.mspayment.model.rqrs.response.ResponseInfo;
import com.example.mspayment.repository.TransactionRepository;
import com.example.mspayment.service.KafkaServices;
import com.example.mspayment.service.PaymentService;
import com.example.mspayment.utils.CommonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class PaymentUsecaseTests {

    @Mock
    private AppProperties appProperties;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private KafkaServices kafkaServices;
    @Spy
    @InjectMocks
    private PaymentUsecase paymentUsecase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createPaymentSuccess() throws Exception {
        RequestInfo requestInfo = CommonUtils.constructRequestInfo(
                "WEB", "unit-test", "unit-test-request-id", "PTRX-1", null);

        ProductTrx productTrx = new ProductTrx();
        productTrx.setTransactionId("PTRX-1");
        productTrx.setPriceCharge(1000L);
        List<ProductTrx> productTrxList = new ArrayList<>();
        productTrxList.add(productTrx);

        Mockito.when(transactionRepository.findProductTrx("PTRX-1")).thenReturn(productTrxList);
        Mockito.when(appProperties.getPAYMENT_CURRENCY()).thenReturn("IDR");
        Mockito.when(appProperties.getPAYMENT_CHECKOUT_METHOD()).thenReturn("ONE_TIME_PAYMENT");

        PaymentRs paymentRs = new PaymentRs();
        Actions actions = new Actions();
        actions.setCheckout_url("http://127.0.0.1:8081/payments/redirect/PTRX-1");
        paymentRs.setAction(actions);
        Mockito.when(paymentService.paymentCharge(any(), any())).thenReturn(paymentRs);

        Mockito.when(appProperties.getORDER_STATUS_READY()).thenReturn("READY");
        Mockito.when(appProperties.getPAYMENT_STATUS_READY()).thenReturn("READY");

        CreatePaymentRq rq = new CreatePaymentRq();
        rq.setCallbackUrl("http://localhost:3000/pay/PTRX-1");

        ResponseInfo<Object> responseInfo = paymentUsecase.createPayment(requestInfo, "SHOPEEPAY", "PTRX-1", rq);
        assertEquals(HttpStatus.OK, responseInfo.getHttpStatus());
        Mockito.verify(paymentService).paymentCharge(any(), any());
    }

    @Test
    void createPaymentTransactionNotFound() throws Exception {
        RequestInfo requestInfo = CommonUtils.constructRequestInfo(
                "WEB", "unit-test", "unit-test-request-id", "PTRX-999", null);

        Mockito.when(transactionRepository.findProductTrx("PTRX-999")).thenReturn(new ArrayList<>());

        CreatePaymentRq rq = new CreatePaymentRq();
        rq.setCallbackUrl("http://localhost:3000/pay/PTRX-999");

        ResponseInfo<Object> responseInfo = paymentUsecase.createPayment(requestInfo, "SHOPEEPAY", "PTRX-999", rq);
        assertEquals(HttpStatus.NOT_FOUND, responseInfo.getHttpStatus());
        Mockito.verify(paymentService, Mockito.never()).paymentCharge(any(), any());
    }
}
