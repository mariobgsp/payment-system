package com.example.mspayment.service;

import com.example.mspayment.config.properties.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class KafkaServices {

    private final AppProperties appProperties;
    private final KafkaTemplate<String, String> createOrderKafkaTemplate;

    public KafkaServices(AppProperties appProperties, KafkaTemplate<String, String> createOrderKafkaTemplate) {
        this.appProperties = appProperties;
        this.createOrderKafkaTemplate = createOrderKafkaTemplate;
    }

    public void publish(String message) throws ExecutionException, InterruptedException {
        SendResult<String, String> sendResult = createOrderKafkaTemplate.send(appProperties.getSERVICELOG_KAFKA_TOPIC() , message).get();
        log.info("Success sent log-event via Kafka, message: {}", message);
        log.info("{}",sendResult.toString());
    }

    public void publishPayment(String topic, String message) throws ExecutionException, InterruptedException {
        SendResult<String, String> sendResult = createOrderKafkaTemplate.send(topic , message).get();
        log.info("Success sent kafka-event via Kafka, topic {}, message: {}", topic, message);
        log.info("{}",sendResult.toString());
    }
}
