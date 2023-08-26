package com.example.mslogger.delivery.listener;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggerListener {
    
    @Qualifier("kafka-topic")
    @KafkaListener(topics = "servicelog", groupId = "service_log")
    public void consume(String message) throws IOException {
        log.info(message);
    }
    
}
