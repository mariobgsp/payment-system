package com.example.mslogger.config.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    
    @Value("${spring.data.mongodb.uri}")
    private String mongoConnectionString;
    @Value("${spring.data.mongodb.database}")
    private String mongoDatabaseName;

    @Bean(name = "mongo-client")
    public MongoClient mongoClient() {
        return MongoClients.create(this.mongoConnectionString);
    }

    @Bean(name = "mongo-template")
    public MongoTemplate MongoTemplate() {
        return new MongoTemplate(mongoClient(), this.mongoDatabaseName);
    }

}
