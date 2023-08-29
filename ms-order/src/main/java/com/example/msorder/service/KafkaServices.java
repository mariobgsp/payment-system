package com.example.msorder.service;

import com.example.msorder.model.kafka.MessageDto;
import com.example.msorder.model.logs.Logs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.example.msorder.config.properties.AppProperties;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class KafkaServices {

    private final KafkaTemplate<String, String> createOrderKafkaTemplate;

    public KafkaServices(KafkaTemplate<String, String> createOrderKafkaTemplate) {
        this.createOrderKafkaTemplate = createOrderKafkaTemplate;
    }

    public void publish(String messageDto) throws ExecutionException, InterruptedException {
        SendResult<String, String> sendResult = createOrderKafkaTemplate.send("servicelogs", messageDto).get();
        log.info("logger {} event sent via Kafka", messageDto);
        log.info(sendResult.toString());
    }
}
