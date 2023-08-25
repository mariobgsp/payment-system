package com.example.mslogger.config.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    @Bean(name = "mongo-client")
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://admin:password@localhost:27017");
    }

    @Bean(name = "mongo-template")
    public MongoTemplate MongoTemplate() {
        return new MongoTemplate(mongoClient(), "servicelog");
    }

}
