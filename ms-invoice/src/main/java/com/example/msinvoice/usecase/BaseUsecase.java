package com.example.msinvoice.usecase;

import com.example.msinvoice.config.properties.AppProperties;
import com.example.msinvoice.models.logs.ServiceLog;
import com.example.msinvoice.models.rqrs.rq.RequestInfo;
import com.example.msinvoice.models.rqrs.rs.ResponseInfo;
import com.example.msinvoice.services.KafkaServices;
import com.example.msinvoice.utils.CommonUtils;
import com.example.msinvoice.utils.LogsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BaseUsecase {

    @Autowired
    private AppProperties appProperties;
    @Autowired
    private KafkaServices kafkaServices;

    public void publish(RequestInfo requestInfo, ResponseInfo<Object> responseInfo){

        // construct logs
        try{
            ServiceLog serviceLog = LogsUtils.construcInsertLogs(requestInfo, responseInfo);
            log.info("Publish log {}", appProperties.isPUBLISH_LOG_KAFKA());
            if(appProperties.isPUBLISH_LOG_KAFKA()){
                kafkaServices.publish(CommonUtils.gson.toJson(serviceLog));
            }
        }catch (Exception e){
            log.error("{}",e.getMessage());
        }
    }


    
}
