package com.example.msorder.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.msorder.config.properties.AppProperties;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaServices {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String message) {
        this.kafkaTemplate.send(appProperties.getSERVICELOG_KAFKA_TOPIC(), message);
        log.info("Success Publishing Message: Topic {}, Message {}", appProperties.getSERVICELOG_KAFKA_TOPIC(), message);
    }
}
