package com.java.project.msorder.usecase;

import com.java.project.msorder.exception.definition.CommonException;
import com.java.project.msorder.exception.handler.ProductNotFoundException;
import com.java.project.msorder.model.repository.Product;
import com.java.project.msorder.model.rqrs.request.RequestInfo;
import com.java.project.msorder.model.rqrs.response.ResponseInfo;
import com.java.project.msorder.repository.StoreRepository;
import com.java.project.msorder.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
public class SimpleUsecase {

    @Autowired
    private StoreRepository storeRepository;
    public ResponseInfo<Object> getAllProduct(RequestInfo requestInfo){
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
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        return responseInfo;
    }


    public ResponseInfo<Object> getAvailableProduct(RequestInfo requestInfo){
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
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        return responseInfo;
    }
}
