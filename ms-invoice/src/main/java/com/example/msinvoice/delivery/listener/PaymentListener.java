package com.example.msinvoice.delivery.listener;

import com.example.msinvoice.config.properties.AppProperties;
import com.example.msinvoice.config.variable.ApplicationConstant;
import com.example.msinvoice.models.rqrs.rq.KafkaMessageRq;
import com.example.msinvoice.models.rqrs.rq.RequestInfo;
import com.example.msinvoice.usecase.FulfillmentUsecase;
import com.example.msinvoice.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service("LogsService")
@Slf4j
public class PaymentListener {
    @Autowired
    private FulfillmentUsecase fulfillmentUsecase;
    @Autowired
    private AppProperties appProperties;

    @KafkaListener(topics = ApplicationConstant.LOGGER_TOPIC, containerFactory=ApplicationConstant.BEAN_PAYMENT_CONTAINER_FACTORY)
    public void logsListener(@Payload String message, Acknowledgment ack) {
        log.info("logger event received, message: {} " , message);
        ack.acknowledge();

        //process
        KafkaMessageRq kafkaMessageRq = CommonUtils.gson.fromJson(message, KafkaMessageRq.class);
        log.info("detail event, id: {} ", kafkaMessageRq.getId());
        log.info("detail event, transactionId: {} ", kafkaMessageRq.getTransactionId());
        log.info("detail event, orderStatus: {} ", kafkaMessageRq.getOrderStatus());
        log.info("detail event, paymentStatus: {} ", kafkaMessageRq.getPaymentStatus());
        log.info("detail event, productCode: {} ", kafkaMessageRq.getProductCode());
        log.info("detail event, productName: {} ", kafkaMessageRq.getProductName());
        log.info("detail event, sysCreationDate: {} ", kafkaMessageRq.getSysCreationDate());
        log.info("detail event, sysUpdateDate: {} ", kafkaMessageRq.getSysUpdateDate());
        log.info("insert log into mongodb ....");

        //construct request info
        RequestInfo request = CommonUtils.constructRequestInfo("kafka", "listen-notify-payment", kafkaMessageRq.getId(), kafkaMessageRq, null);

        if(kafkaMessageRq.getOrderStatus().equals(appProperties.getORDER_STATUS_PUBLISHED())&&kafkaMessageRq.getPaymentStatus().equals(appProperties.getPAYMENT_STATUS_SUCCESS())){
            CompletableFuture.runAsync(()->fulfillmentUsecase.fulfillProduct(request, kafkaMessageRq));
        }else {
            log.info("Cannot be processed, orderstatus: {} & paymentstatus: {}", kafkaMessageRq.getOrderStatus(), kafkaMessageRq.getPaymentStatus());
        }

    }
}
