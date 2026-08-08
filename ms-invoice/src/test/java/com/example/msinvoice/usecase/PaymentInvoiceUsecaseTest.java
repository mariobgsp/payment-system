package com.example.msinvoice.usecase;

import com.example.msinvoice.model.repository.ProductTrx;
import com.example.msinvoice.model.rqrs.request.RequestInfo;
import com.example.msinvoice.model.rqrs.response.ResponseInfo;
import com.example.msinvoice.repository.ProductTrxRepository;
import com.example.msinvoice.utils.CommonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentInvoiceUsecaseTest {

    @Mock
    private ProductTrxRepository productTrxRepository;
    @Spy
    @InjectMocks
    private PaymentInvoiceUsecase paymentInvoiceUsecase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processTransactionNotFound() {
        RequestInfo request = CommonUtils.constructRequestInfo("kafka-listener", "provision-notify-payment", "PTRX-1", null, null);
        Mockito.when(productTrxRepository.findByTransactionId("PTRX-1")).thenReturn(Optional.empty());

        paymentInvoiceUsecase.process(request, "PTRX-1");

        Mockito.verify(productTrxRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void processSuccess() {
        RequestInfo request = CommonUtils.constructRequestInfo("kafka-listener", "provision-notify-payment", "PTRX-1", null, null);
        ProductTrx productTrx = new ProductTrx();
        productTrx.setId("id-1");
        productTrx.setTransactionId("PTRX-1");
        productTrx.setPrice(new java.math.BigDecimal("1000"));
        productTrx.setPaymentDate(new java.util.Date());
        Mockito.when(productTrxRepository.findByTransactionId("PTRX-1")).thenReturn(Optional.of(productTrx));

        paymentInvoiceUsecase.process(request, "PTRX-1");

        Mockito.verify(productTrxRepository).save(Mockito.any());
        assertEquals("SUCCESS", productTrx.getOrderStatus());
    }
}
