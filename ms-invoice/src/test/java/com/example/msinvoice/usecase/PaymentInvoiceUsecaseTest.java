package com.example.msinvoice.usecase;

import com.example.msinvoice.model.rqrs.request.RequestInfo;
import com.example.msinvoice.utils.CommonUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
class PaymentInvoiceUsecaseTest {

    @Autowired
    private PaymentInvoiceUsecase invoiceUsecase;

    @Test
    void process() {
        RequestInfo request = CommonUtils.constructRequestInfo("kafka-listener", "provision-notify-payment", "PTRX-1", null, null);
        invoiceUsecase.process(request, "PTRX-1");
    }
}