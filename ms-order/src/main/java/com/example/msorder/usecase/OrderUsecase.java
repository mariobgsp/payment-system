package com.example.msorder.usecase;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.msorder.config.properties.AppProperties;
import com.example.msorder.exception.definition.CommonException;
import com.example.msorder.exception.definition.BadRequestException;
import com.example.msorder.exception.definition.ProductNotFoundException;
import com.example.msorder.exception.definition.TransactionNotFoundException;
import com.example.msorder.exception.definition.UserNotFoundException;
import com.example.msorder.model.repository.Product;
import com.example.msorder.model.repository.ProductTrx;
import com.example.msorder.model.repository.StoreUser;
import com.example.msorder.model.rqrs.request.RequestInfo;
import com.example.msorder.model.rqrs.request.order.OrderRq;
import com.example.msorder.model.rqrs.response.ResponseInfo;
import com.example.msorder.model.rqrs.response.ViewProductRs;
import com.example.msorder.model.rqrs.response.order.OrderRs;
import com.example.msorder.repository.LogRepository;
import com.example.msorder.repository.StoreRepository;
import com.example.msorder.utils.CommonUtils;
import com.example.msorder.utils.ResponseUtils;
import com.example.msorder.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Component
@Slf4j
public class OrderUsecase extends BaseUsecase{

    @Autowired
    private AppProperties appProperties;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private LogRepository logRepository;

    public ResponseInfo<Object> viewProduct(RequestInfo requestInfo, String username){
        log.info("[{} - viewProduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // check user
            List<StoreUser> storeUsers = storeRepository.getUserDetail(username);
            if(storeUsers.size()==0){
                throw new UserNotFoundException("01", "User not allowed or not available to view product");
            }
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
                productList = storeRepository.getSpecialProduct(storeUser.isSpecialProduct());
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

            // set success
            responseInfo = ResponseUtils.generateSuccessRs(requestInfo, viewProductRs);
        }catch (Exception e){
            log.error("[{} - getAllproduct][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        log.info("[{} - viewProduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        super.publish(requestInfo, responseInfo);
        return responseInfo;
    }


    public ResponseInfo<Object> getUserInfo(RequestInfo requestInfo, String username){
        log.info("[{} - getUserInfo][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // get user detail
            List<StoreUser> storeUsers = storeRepository.getUserDetail(username);
            if(storeUsers.size()==0){
                throw new UserNotFoundException("01", "User not allowed or not available to view product");
            }
            //set success
            responseInfo = ResponseUtils.generateSuccessRs(requestInfo, storeUsers);
        }catch (Exception e){
            log.error("[{} - getUserInfo][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }   
        log.info("[{} - getUserInfo][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData()); 
        super.publish(requestInfo, responseInfo);
        return responseInfo;
    }


    public ResponseInfo<Object> orderProduct(RequestInfo requestInfo, String username, OrderRq bodyRq){
        log.info("[{} - orderProduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();
        try{
            // validate request body
            if(StringUtils.isEmpty(bodyRq.getProductCode())
                    || StringUtils.isEmpty(bodyRq.getProductName())
                    || StringUtils.isEmpty(bodyRq.getUserDetail().getUsername())
                    || StringUtils.isEmpty(bodyRq.getUserDetail().getPassword())){
                throw new BadRequestException("03", "Invalid value should not be empty");
            }

            // validate users
            if(!username.equals(bodyRq.getUserDetail().getUsername())){
                throw new BadRequestException("04", "Invalid username with req body");
            }
            List<StoreUser> storeUsers = storeRepository.getUserDetail(username);
            if(storeUsers.size()==0){
                throw new UserNotFoundException("01", "User not allowed or not available to view product");
            }
            String hashPassword = SecurityUtils.encodeRequestBody(bodyRq.getUserDetail().getPassword(), appProperties.getSECRET_KEY());
            if(!hashPassword.equals(storeUsers.get(0).getHashPassword())){
                throw new BadRequestException("05", "Invalid password");
            }

            // check product if available
            List<Product> productList = storeRepository.getSingleProduct(bodyRq.getProductCode());
            if(productList.size()==0){
                throw new ProductNotFoundException("02", "Product Not Available");
            }
            Product product = productList.get(0);
            if(product.getPrice()!=bodyRq.getPrice()){
                throw new BadRequestException("06", String.format("Price not match should be %s", product.getPrice()));
            }
            long price = 0L;
            if(product.isEnableDiscount() && bodyRq.isEnableDiscount()){
                // calculate discount
                long discount = (long) (product.getDiscount() * 100);
                price =  bodyRq.getPrice() - (bodyRq.getPrice() * discount / 100);
            }else if (!product.isEnableDiscount() && bodyRq.isEnableDiscount()){
                throw new BadRequestException("07", "discount not available, turn off enable discount flag");
            }else {
                price = bodyRq.getPrice();
            }

            // count total charge
            price = price * bodyRq.getAmount();

            // construct transaction Id
            String transactionId = "PTRX-"+ logRepository.getSequence(requestInfo);

            // construct insert model
            ProductTrx productTrx = new ProductTrx()
                    .setId("MSO-"+UUID.randomUUID())
                    .setTransactionId(transactionId)
                    .setOrderStatus(appProperties.getORDER_STATUS_CREATED())
                    .setPaymentStatus(appProperties.getPAYMENT_STATUS_CREATED())
                    .setUserId(storeUsers.get(0).getUserId())
                    .setProductName(product.getProductName())
                    .setAmount((long) bodyRq.getAmount())
                    .setPrice((long) bodyRq.getPrice())
                    .setPriceCharge(price)
                    .setProductCode(product.getProductCode())
                    .setParam_1(null)
                    .setParam_2(null)
                    .setDiscount(product.getDiscount())
                    .setDiscountEnabled(bodyRq.isEnableDiscount());

            // insert into transaction database
            logRepository.insertProductTrx(requestInfo, productTrx);

            OrderRs orderRs = new OrderRs()
                    .setCreatedAt(CommonUtils.dateFormatter(new Date()))
                    .setTransactionId(transactionId);

            // set response
            responseInfo = ResponseUtils.generateSuccessRs(requestInfo, orderRs);
        }catch (Exception e){
            log.error("[{} - orderProduct][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        log.info("[{} - orderProduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        super.publish(requestInfo, responseInfo);
        return responseInfo;
    }

    public ResponseInfo<Object> checkProduct(RequestInfo requestInfo, String username, String transactionId){
        log.info("[{} - checkProduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        ResponseInfo<Object> responseInfo = new ResponseInfo<>();

        try{
            // check user
            List<StoreUser> storeUsers = storeRepository.getUserDetail(username);
            if(storeUsers.size()==0){
                throw new UserNotFoundException("01", "User not exist");
            }
            List<ProductTrx> productTrxes = logRepository.getProductTrx(requestInfo, transactionId);
            if(productTrxes.size()==0){
                throw new TransactionNotFoundException("08", "Transaction not exist");
            }

            responseInfo = ResponseUtils.generateSuccessRs(requestInfo, productTrxes);
        }catch (Exception e){
            log.error("[{} - checkProduct][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        log.info("[{} - checkProduct][{}][{}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData());
        super.publish(requestInfo, responseInfo);
        return responseInfo;
    }

}
