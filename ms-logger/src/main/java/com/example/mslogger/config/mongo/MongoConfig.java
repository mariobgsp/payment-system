package com.example.mslogger.config.mongo;

import com.example.mslogger.config.properties.AppProperties;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

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
