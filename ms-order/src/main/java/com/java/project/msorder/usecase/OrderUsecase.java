package com.java.project.msorder.usecase;

import com.java.project.msorder.config.properties.AppProperties;
import com.java.project.msorder.exception.definition.CommonException;
import com.java.project.msorder.exception.handler.BadRequestException;
import com.java.project.msorder.exception.handler.ProductNotFoundException;
import com.java.project.msorder.exception.handler.UserNotFoundException;
import com.java.project.msorder.model.repository.Product;
import com.java.project.msorder.model.repository.ProductTrx;
import com.java.project.msorder.model.repository.StoreUser;
import com.java.project.msorder.model.rqrs.request.RequestInfo;
import com.java.project.msorder.model.rqrs.request.order.OrderRq;
import com.java.project.msorder.model.rqrs.response.ResponseInfo;
import com.java.project.msorder.model.rqrs.response.ViewProductRs;
import com.java.project.msorder.model.rqrs.response.order.OrderRs;
import com.java.project.msorder.repository.ProductTrxRepository;
import com.java.project.msorder.repository.StoreRepository;
import com.java.project.msorder.utils.CommonUtils;
import com.java.project.msorder.utils.ResponseUtils;
import com.java.project.msorder.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Component
@Slf4j
public class OrderUsecase {

    private AppProperties appProperties;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private ProductTrxRepository productTrxRepository;

    public ResponseInfo<Object> viewProduct(RequestInfo requestInfo, String username){
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
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }

        return responseInfo;
    }


    public ResponseInfo<Object> getUserInfo(RequestInfo requestInfo, String username){
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
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        return responseInfo;
    }


    public ResponseInfo<Object> orderProduct(RequestInfo requestInfo, String username, OrderRq bodyRq){
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
            String transactionId = "PTRX-"+productTrxRepository.getSequence(requestInfo);

            // construct insert model
            ProductTrx productTrx = new ProductTrx()
                    .setId("MSO-"+UUID.randomUUID())
                    .setTransactionId(transactionId)
                    .setUserId(storeUsers.get(0).getUserId())
                    .setProductName(product.getProductName())
                    .setAmount((long) bodyRq.getAmount())
                    .setPrice((long) bodyRq.getPrice())
                    .setPriceCharge(price)
                    .setProductCode(product.getProductCode())
                    .setParam_1(null)
                    .setParam_2(null)
                    .setDiscountEnabled(bodyRq.isEnableDiscount());

            // insert into transaction database
            productTrxRepository.insertProductTrx(requestInfo, productTrx);

            OrderRs orderRs = new OrderRs()
                    .setCreatedAt(CommonUtils.dateFormatter(new Date()))
                    .setTransactionId(transactionId);

            // set response
            responseInfo = ResponseUtils.generateSuccessRs(requestInfo, orderRs);
        }catch (Exception e){
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            responseInfo = ResponseUtils.generateException(requestInfo, ex);
        }
        return responseInfo;
    }

}