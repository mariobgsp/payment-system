package com.example.msinvoice.delivery.eventlistener;

import com.example.msinvoice.config.variable.ApplicationConstant;
import com.example.msinvoice.model.kafka.KafkaMessageRq;
import com.example.msinvoice.model.rqrs.request.RequestInfo;
import com.example.msinvoice.usecase.PaymentInvoiceUsecase;
import com.example.msinvoice.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service("PaymentServices")
@Slf4j
public class PaymentEventListener {

    @Autowired
    private PaymentInvoiceUsecase paymentInvoiceUsecase;

    @KafkaListener(topics = "ms-notify-payment", containerFactory=ApplicationConstant.BEAN_PAYMENT_EVENT_CONTAINER_FACTORY)
    public void paymentListener(@Payload String message, Acknowledgment ack) {
        log.info("logger event received, message: {} ", message);

        try {
            // process
            KafkaMessageRq kafkaMessageRq = CommonUtils.gson.fromJson(message, KafkaMessageRq.class);
            log.info("detail payment event, transactionId: {} ", kafkaMessageRq.getTransactionId());
            log.info("detail payment event, paymentStatus: {} ", kafkaMessageRq.getPaymentStatus());
            log.info("detail payment event, orderStatus: {} ", kafkaMessageRq.getOrderStatus());

            // construct request info
            RequestInfo request = CommonUtils.constructRequestInfo("kafka-listener", "provision-notify-payment", kafkaMessageRq.getTransactionId(), kafkaMessageRq, null);

            if(kafkaMessageRq.getOrderStatus().equals("PUBLISHED") && kafkaMessageRq.getPaymentStatus().equals("SUCCESS")){
                paymentInvoiceUsecase.process(request, kafkaMessageRq.getTransactionId());
            }
        } finally {
            // acknowledge only after processing completes, to avoid losing events
            ack.acknowledge();
        }
    }

}
