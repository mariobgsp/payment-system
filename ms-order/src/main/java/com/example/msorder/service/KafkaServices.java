package com.example.msorder.service;

import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.example.msorder.config.properties.AppProperties;

import lombok.extern.slf4j.Slf4j;

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
}
