package com.example.msinvoice.usecase;

import com.example.msinvoice.config.properties.AppProperties;
import com.example.msinvoice.exception.CommonException;
import com.example.msinvoice.exception.TrxNotFoundException;
import com.example.msinvoice.models.repository.ProductTrx;
import com.example.msinvoice.models.rqrs.rq.KafkaMessageRq;
import com.example.msinvoice.models.rqrs.rq.RequestInfo;
import com.example.msinvoice.models.rqrs.rs.ResponseInfo;
import com.example.msinvoice.repository.impl.TransactionImplementationRepository;
import com.example.msinvoice.repository.impl.StoreImplementationRepository;
import com.example.msinvoice.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FulfillmentUsecase extends BaseUsecase{

    @Autowired
    private AppProperties appProperties;
    @Autowired
    private StoreImplementationRepository storeImplementationRepository;
    @Autowired
    private TransactionImplementationRepository transactionImplementationRepository;
    public void fulfillProduct(RequestInfo requestInfo, KafkaMessageRq kafkaMessageRq){
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // find trx in product_trx
            List<ProductTrx> productTrxList = transactionImplementationRepository.findProductTrx(kafkaMessageRq.getTransactionId());
            if (productTrxList.isEmpty()){
                throw new TrxNotFoundException("01", "transaction not found");
            }

            // check user info /v1/{username}/user-detail

            // process


        }catch (Exception e){
            log.error("[{} - fulfillProduct][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        // publish log
        super.publish(requestInfo, responseInfo);
    }

}
