package com.example.mspayment.usecase;

import com.example.mspayment.config.properties.AppProperties;
import com.example.mspayment.model.logs.ServiceLog;
import com.example.mspayment.model.rqrs.request.RequestInfo;
import com.example.mspayment.model.rqrs.response.ResponseInfo;
import com.example.mspayment.service.KafkaServices;
import com.example.mspayment.utils.CommonUtils;
import com.example.mspayment.utils.LogsUtils;
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
