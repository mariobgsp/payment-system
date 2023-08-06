package com.java.project.msorder.usecase;

import com.java.project.msorder.model.repository.Product;
import com.java.project.msorder.model.repository.StoreUser;
import com.java.project.msorder.model.rqrs.request.RequestInfo;
import com.java.project.msorder.model.rqrs.response.Response;
import com.java.project.msorder.model.rqrs.response.ResponseInfo;
import com.java.project.msorder.model.rqrs.response.ViewProductRs;
import com.java.project.msorder.repository.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class OrderUsecase {

    @Autowired
    private StoreRepository storeRepository;


    public ResponseInfo<Object> viewProduct(RequestInfo requestInfo, String username){
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // check user
            List<StoreUser> storeUsers = storeRepository.getUserDetail(username);
            StoreUser storeUser = storeUsers.get(0);

            List<Product> productList = new ArrayList<>();
            // get product
            if(storeUser.isSpecialProduct()){
                log.info("Special users: {}", username);
                // get all product
                productList = storeRepository.getAllProduct();
            }else{
                log.info("Normal users: {}", username);
                // assign to get false special product
                productList = storeRepository.getSpecificProduct(String.valueOf(storeUser.isSpecialProduct()));
            }

            // gather all product
            // not all column can be viewed
            List<ViewProductRs> viewProductRs = new ArrayList<>();
            for (Product pr: productList) {
                ViewProductRs vp = new ViewProductRs();
                vp.setProductCode(pr.getProductCode());
                vp.setProductName(pr.getProductName());
                vp.setPrice(pr.getPrice());
                vp.setDiscount(pr.getDiscount());
                vp.setDiscountAvailable(pr.isEnableDiscount());
                vp.setProductUpdateDate(pr.getProductUpdate());
                vp.setProductInsertDate(pr.getProductInsert());
                viewProductRs.add(vp);
            }

            // construct response
            Response<Object> response = new Response<>();
            response.setCode("00");
            response.setStatus("ok");
            response.setMessage("request-success");
            response.setData(viewProductRs);

            responseInfo.setHttpStatus(HttpStatus.OK);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-request-id", requestInfo.getRequestId());
            httpHeaders.add("x-channel-id", requestInfo.getChannel());
            httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
            responseInfo.setHttpHeaders(httpHeaders);
            responseInfo.setResponse(response);

        }catch (Exception e){
            Response<Object> response = new Response<>();
            response.setCode("99");
            response.setStatus("failed");
            response.setMessage(String.format("failed, cause:%s", e.getMessage()));
            response.setData(null);

            responseInfo.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-request-id", requestInfo.getRequestId());
            httpHeaders.add("x-channel-id", requestInfo.getChannel());
            httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
            responseInfo.setHttpHeaders(httpHeaders);
            responseInfo.setResponse(response);
        }

        return responseInfo;
    }

    public ResponseInfo<Object> getAllProduct(RequestInfo requestInfo){
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // fetch all product
            List<Product> productList = storeRepository.getAllProduct();

            Response<Object> response = new Response<>();
            response.setCode("00");
            response.setStatus("ok");
            response.setMessage("request-success");
            response.setData(productList);

            responseInfo.setHttpStatus(HttpStatus.OK);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-request-id", requestInfo.getRequestId());
            httpHeaders.add("x-channel-id", requestInfo.getChannel());
            httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
            responseInfo.setHttpHeaders(httpHeaders);
            responseInfo.setResponse(response);

        }catch (Exception e){

            Response<Object> response = new Response<>();
            response.setCode("99");
            response.setStatus("failed");
            response.setMessage(String.format("failed, cause:%s", e.getMessage()));
            response.setData(null);

            responseInfo.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-request-id", requestInfo.getRequestId());
            httpHeaders.add("x-channel-id", requestInfo.getChannel());
            httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
            responseInfo.setHttpHeaders(httpHeaders);
            responseInfo.setResponse(response);

        }
        return responseInfo;
    }


    public ResponseInfo<Object> getAvailableProduct(RequestInfo requestInfo){
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // fetch all product
            List<Product> productList = storeRepository.getAvailableProduct();

            Response<Object> response = new Response<>();
            response.setCode("00");
            response.setStatus("ok");
            response.setMessage("request-success");
            response.setData(productList);

            responseInfo.setHttpStatus(HttpStatus.OK);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-request-id", requestInfo.getRequestId());
            httpHeaders.add("x-channel-id", requestInfo.getChannel());
            httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
            responseInfo.setHttpHeaders(httpHeaders);
            responseInfo.setResponse(response);

        }catch (Exception e){

            Response<Object> response = new Response<>();
            response.setCode("99");
            response.setStatus("failed");
            response.setMessage(String.format("failed, cause:%s", e.getMessage()));
            response.setData(null);

            responseInfo.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-request-id", requestInfo.getRequestId());
            httpHeaders.add("x-channel-id", requestInfo.getChannel());
            httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
            responseInfo.setHttpHeaders(httpHeaders);
            responseInfo.setResponse(response);

        }
        return responseInfo;
    }


    public ResponseInfo<Object> getUserInfo(RequestInfo requestInfo, String username){
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // get user detail
            List<StoreUser> storeUsers = storeRepository.getUserDetail(username);

            Response<Object> response = new Response<>();
            response.setCode("00");
            response.setStatus("ok");
            response.setMessage("request-success");
            response.setData(storeUsers);

            responseInfo.setHttpStatus(HttpStatus.OK);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-request-id", requestInfo.getRequestId());
            httpHeaders.add("x-channel-id", requestInfo.getChannel());
            httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
            responseInfo.setHttpHeaders(httpHeaders);
            responseInfo.setResponse(response);

        }catch (Exception e){

            Response<Object> response = new Response<>();
            response.setCode("99");
            response.setStatus("failed");
            response.setMessage(String.format("failed, cause:%s", e.getMessage()));
            response.setData(null);

            responseInfo.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-request-id", requestInfo.getRequestId());
            httpHeaders.add("x-channel-id", requestInfo.getChannel());
            httpHeaders.add("x-request-at", String.valueOf(requestInfo.getRequestAt()));
            responseInfo.setHttpHeaders(httpHeaders);
            responseInfo.setResponse(response);

        }
        return responseInfo;
    }

}
