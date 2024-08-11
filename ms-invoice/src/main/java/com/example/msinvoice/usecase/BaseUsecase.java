package com.example.msinvoice.usecase;

import com.example.msinvoice.config.properties.AppProperties;
import com.example.msinvoice.model.logs.ServiceLog;
import com.example.msinvoice.model.rqrs.request.RequestInfo;
import com.example.msinvoice.model.rqrs.response.ResponseInfo;
import com.example.msinvoice.service.KafkaService;
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
    private KafkaService kafkaServices;

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
