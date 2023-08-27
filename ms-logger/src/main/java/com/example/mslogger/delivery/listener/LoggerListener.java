package com.example.mslogger.delivery.listener;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.mslogger.model.repository.Logs;
import com.example.mslogger.repository.MongoRepository;
import com.example.mslogger.utils.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LoggerListener {
    
    @Autowired
    private MongoRepository mongoRepository;

    @Qualifier("kafka-topic")
    @KafkaListener(topics = "servicelog", groupId = "service_log")
    public void consume(String message) throws IOException {
        log.info("Receiving message: {}",message);
        
        Logs logs = CommonUtils.gson.fromJson(message, Logs.class);
        mongoRepository.insertToMongodb(logs);
    }
    
}
