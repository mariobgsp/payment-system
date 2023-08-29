package com.example.mslogger.delivery.listener;

import java.io.IOException;

import com.example.mslogger.model.kafka.MessageDto;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.example.mslogger.model.repository.Logs;
import com.example.mslogger.repository.MongoRepository;
import com.example.mslogger.utils.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Service("LogsService")
@Slf4j
public class LoggerListener {
    
    @Autowired
    private MongoRepository mongoRepository;

    @KafkaListener(topics = "servicelogs", containerFactory="LogsContainerFactory")
    public void logsListener(@Payload String messageDto, Acknowledgment ack) {
        log.info("logger service received order {} ", messageDto);
        ack.acknowledge();

        // process
        MessageDto messageDt = CommonUtils.gson.fromJson(messageDto, MessageDto.class);
        Logs logs = CommonUtils.gson.fromJson(messageDt.getMessage(), Logs.class);
        mongoRepository.insertToMongodb(logs);
    }
    
}
