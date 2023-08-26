package com.example.mslogger.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.example.mslogger.config.properties.AppProperties;

@Configuration
public class KafkaConfig {

    @Autowired
    private AppProperties appProperties;
    
    @Bean(name = "kafka-topic")
    public NewTopic taskTopic() {
    return TopicBuilder.name(appProperties.getSERVICELOG_KAFKA_TOPIC())
        .partitions(1)
        .replicas(1)
        .build();
    }

}
