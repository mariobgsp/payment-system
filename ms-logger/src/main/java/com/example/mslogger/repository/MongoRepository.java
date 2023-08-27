package com.example.mslogger.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class MongoRepository {

    @Autowired
    @Qualifier("mongo-template")
    private MongoTemplate mongoTemplate;

    public void insertToMongodb(Object object){
        try{
            // insert
            mongoTemplate.insert(object);
            log.info("success insert logs, {}", object);
        }catch(Exception e){
            log.error("error insert to mongodb {}", e.getMessage());
        }
    }

}
