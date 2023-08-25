package com.example.msorder.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.msorder.exception.definition.CommonException;
import com.example.msorder.exception.handler.ProductNotFoundException;
import com.example.msorder.model.repository.Product;
import com.example.msorder.model.rqrs.request.RequestInfo;
import com.example.msorder.model.rqrs.response.ResponseInfo;
import com.example.msorder.repository.StoreRepository;
import com.example.msorder.utils.ResponseUtils;

import java.util.List;


@Component
@Slf4j
public class SimpleUsecase {

    @Autowired
    private StoreRepository storeRepository;
    public ResponseInfo<Object> getAllProduct(RequestInfo requestInfo){
        log.info("[{} - getAllproduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // fetch all product
            List<Product> productList = storeRepository.getAllProduct();
            if(productList.size()==0){
                throw new ProductNotFoundException("02", "Product Not Available");
            }
            //set success
            responseInfo = ResponseUtils.generateSuccessRs(requestInfo, productList);
        }catch (Exception e){
            log.error("[{} - getAllproduct][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        log.info("[{} - getAllproduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        return responseInfo;
    }


    public ResponseInfo<Object> getAvailableProduct(RequestInfo requestInfo){
        log.info("[{} - getAvailableProduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // fetch all product
            List<Product> productList = storeRepository.getAvailableProduct();
            if(productList.size()==0){
                throw new ProductNotFoundException("02", "Product Not Available");
            }
            //set success
            responseInfo = ResponseUtils.generateSuccessRs(requestInfo, productList);
        }catch (Exception e){
            log.error("[{} - getAvailableProduct][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        log.info("[{} - getAvailableProduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        return responseInfo;
    }
}
