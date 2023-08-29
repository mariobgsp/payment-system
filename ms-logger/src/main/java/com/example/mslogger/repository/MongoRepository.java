package com.example.mslogger.repository;

import com.example.mslogger.config.variable.ApplicationConstant;
import com.example.mslogger.model.repository.ServiceLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class MongoRepository {

    @Autowired
    @Qualifier(ApplicationConstant.BEAN_MONGO_TEMPLATE)
    private MongoTemplate mongoTemplate;

    public void insertToMongodb(ServiceLog serviceLog){
        try{

            // insert
            mongoTemplate.insert(serviceLog);
            MongoRepository.log.info("success insert serviceLog, {}", serviceLog.getRequestId());
        }catch(Exception e){
            MongoRepository.log.error("error insert to mongodb {}", e.getMessage());
        }
    }

}
