package com.example.mslogger.service;

import com.example.mslogger.model.kafka.MessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class KafkaService {

    private final KafkaTemplate<String, String> createOrderKafkaTemplate;


    public KafkaService(KafkaTemplate<String, String> createOrderKafkaTemplate) {
        this.createOrderKafkaTemplate = createOrderKafkaTemplate;
    }

    public void publish(String messageDto) throws ExecutionException, InterruptedException {
        SendResult<String, String> sendResult = createOrderKafkaTemplate.send("servicelogs", messageDto).get();
        log.info("logger {} event sent via Kafka", messageDto);
        log.info(sendResult.toString());
    }
}
