package com.example.mslogger.delivery.listener;

import com.example.mslogger.config.variable.ApplicationConstant;
import com.example.mslogger.model.repository.ServiceLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.example.mslogger.repository.MongoRepository;
import com.example.mslogger.utils.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Service("LogsService")
@Slf4j
public class LoggerListener {
    
    @Autowired
    private MongoRepository mongoRepository;

    @KafkaListener(topics = ApplicationConstant.LOGGER_TOPIC, containerFactory=ApplicationConstant.BEAN_LOG_CONTAINER_FACTORY)
    public void logsListener(@Payload String message, Acknowledgment ack) {
        log.info("logger event received, message: {} ", message);
        ack.acknowledge();

        // process
        ServiceLog serviceLog = CommonUtils.gson.fromJson(message, ServiceLog.class);
        log.info("detail event, appName: {} ", serviceLog.getAppName());
        log.info("detail event, operationName: {} ", serviceLog.getOperationName());
        log.info("detail event, requestId: {} ", serviceLog.getRequestId());
        log.info("detail event, requestAt: {} ", serviceLog.getRequestAt());
        log.info("detail event, completionStatus: {} ", serviceLog.getCompletionStatus());
        log.info("insert log into mongodb ....");
        mongoRepository.insertToMongodb(serviceLog);
    }
    
}
