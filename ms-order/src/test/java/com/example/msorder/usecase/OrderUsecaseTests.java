package com.example.msorder.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;

import com.example.msorder.config.properties.AppProperties;
import com.example.msorder.model.repository.Product;
import com.example.msorder.model.repository.ProductTrx;
import com.example.msorder.model.repository.StoreUser;
import com.example.msorder.model.rqrs.request.RequestInfo;
import com.example.msorder.model.rqrs.request.order.OrderRq;
import com.example.msorder.model.rqrs.request.order.UserDetail;
import com.example.msorder.model.rqrs.response.ResponseInfo;
import com.example.msorder.repository.LogRepository;
import com.example.msorder.repository.StoreRepository;
import com.example.msorder.utils.CommonUtils;
import com.example.msorder.utils.SecurityUtils;

public class OrderUsecaseTests {

    @Mock
    private AppProperties appProperties;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private LogRepository logRepository;
    @Spy
    @InjectMocks
    private OrderUsecase orderUsecase;

    @BeforeEach
    void setUp () {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void viewProduct() throws Exception {

        // init request info
        RequestInfo requestInfo = CommonUtils.constructRequestInfo(
            "W", 
            "unit-test", 
            "unit-test-request-id", 
            "test-username", 
            null);

        // init store user
        StoreUser storeUser = new StoreUser();
        storeUser.setSpecialProduct(true);
        List<StoreUser> lStoreUsers = new ArrayList<>();
        lStoreUsers.add(storeUser);

        // init product 
        Product product = new Product();
        product.setProductCode("ID123");
        product.setProductName("productname");
        product.setPrice(1);
        product.setDiscount(0D);
        product.setEnableDiscount(false);
        product.setProductUpdate(null);
        product.setProductInsert(null);

        List<Product> products = new ArrayList<>();

        // mock product repository
        Mockito.doReturn(products).when(storeRepository).getAllProduct();

        // mock store repository
        Mockito.doReturn(lStoreUsers).when(storeRepository).getUserDetail(Mockito.any());
        
        // init unit test
        ResponseInfo<Object> responseInfo = orderUsecase.viewProduct(requestInfo, "test-username");
        // assert
        assertEquals(HttpStatus.OK, responseInfo.getHttpStatus());
    }

    @Test
    void getUserInfo() {

        // init request info
        RequestInfo requestInfo = CommonUtils.constructRequestInfo(
            "W", 
            "unit-test", 
            "unit-test-request-id", 
            "test-username", 
            null);

        // init store user
        StoreUser storeUser = new StoreUser();
        storeUser.setSpecialProduct(true);
        List<StoreUser> lStoreUsers = new ArrayList<>();
        lStoreUsers.add(storeUser);

        // mock store repository
        Mockito.doReturn(lStoreUsers).when(storeRepository).getUserDetail(Mockito.any());

        // init unit test
        ResponseInfo<Object> responseInfo = orderUsecase.getUserInfo(requestInfo, "test-username");
        // assert
        assertEquals(HttpStatus.OK, responseInfo.getHttpStatus());
    }

    @Test
    void orderProductWithValidToken() throws Exception {
        // init request info
        RequestInfo requestInfo = CommonUtils.constructRequestInfo(
            "W", 
            "unit-test", 
            "unit-test-request-id", 
            "test-username", 
            null);

        // init store user with a legacy HMAC hash of "secretpass"
        StoreUser storeUser = new StoreUser();
        storeUser.setSpecialProduct(true);
        storeUser.setUserId("UID-1");
        storeUser.setUserName("test-username");
        storeUser.setHashPassword(SecurityUtils.encodeRequestBody("secretpass", "test-secret"));
        List<StoreUser> lStoreUsers = new ArrayList<>();
        lStoreUsers.add(storeUser);

        // init product
        Product product = new Product();
        product.setProductCode("ID123");
        product.setProductName("productname");
        product.setPrice(1000);
        product.setDiscount(0D);
        product.setEnableDiscount(false);

        List<Product> products = new ArrayList<>();
        products.add(product);

        Mockito.when(appProperties.getSECRET_KEY()).thenReturn("test-secret");
        Mockito.when(storeRepository.getUserDetail("test-username")).thenReturn(lStoreUsers);
        Mockito.when(storeRepository.getSingleProduct("ID123")).thenReturn(products);
        Mockito.when(logRepository.getSequence(Mockito.any())).thenReturn(1L);
        Mockito.when(appProperties.getORDER_STATUS_CREATED()).thenReturn("CREATED");
        Mockito.when(appProperties.getPAYMENT_STATUS_CREATED()).thenReturn("CREATED");

        OrderRq orderRq = new OrderRq();
        orderRq.setProductCode("ID123");
        orderRq.setProductName("productname");
        orderRq.setAmount(2);
        orderRq.setPrice(1000);
        orderRq.setEnableDiscount(false);
        UserDetail userDetail = new UserDetail();
        userDetail.setUsername("test-username");
        userDetail.setPassword("secretpass");
        orderRq.setUserDetail(userDetail);

        ResponseInfo<Object> responseInfo = orderUsecase.orderProduct(requestInfo, "test-username", orderRq, null);
        assertEquals(HttpStatus.OK, responseInfo.getHttpStatus());
        Mockito.verify(logRepository).insertProductTrx(Mockito.any(), Mockito.any());
    }

    @Test
    void orderProductRejectsWrongPassword() throws Exception {
        RequestInfo requestInfo = CommonUtils.constructRequestInfo(
            "W",
            "unit-test",
            "unit-test-request-id",
            "test-username",
            null);

        StoreUser storeUser = new StoreUser();
        storeUser.setSpecialProduct(true);
        storeUser.setUserId("UID-1");
        storeUser.setUserName("test-username");
        storeUser.setHashPassword(SecurityUtils.encodeRequestBody("secretpass", "test-secret"));
        List<StoreUser> lStoreUsers = new ArrayList<>();
        lStoreUsers.add(storeUser);

        Mockito.when(appProperties.getSECRET_KEY()).thenReturn("test-secret");
        Mockito.when(storeRepository.getUserDetail("test-username")).thenReturn(lStoreUsers);

        OrderRq orderRq = new OrderRq();
        orderRq.setProductCode("ID123");
        orderRq.setProductName("productname");
        orderRq.setAmount(1);
        orderRq.setPrice(1000);
        UserDetail userDetail = new UserDetail();
        userDetail.setUsername("test-username");
        userDetail.setPassword("wrongpass");
        orderRq.setUserDetail(userDetail);

        ResponseInfo<Object> responseInfo = orderUsecase.orderProduct(requestInfo, "test-username", orderRq, null);
        assertEquals(HttpStatus.BAD_REQUEST, responseInfo.getHttpStatus());
        Mockito.verify(logRepository, Mockito.never()).insertProductTrx(Mockito.any(), Mockito.any());
    }

    @Test
    void checkProduct() throws Exception{

        // init request info
        RequestInfo requestInfo = CommonUtils.constructRequestInfo(
            "W", 
            "unit-test", 
            "unit-test-request-id", 
            "test-username", 
            null);

        // init store user
        StoreUser storeUser = new StoreUser();
        storeUser.setSpecialProduct(true);
        List<StoreUser> lStoreUsers = new ArrayList<>();
        lStoreUsers.add(storeUser);

        // init product Trx
        ProductTrx productTrx = new ProductTrx();
        productTrx.setAmount(1L);
        List<ProductTrx> productTrxs = new ArrayList<>(0);
        productTrxs.add(productTrx);

        // mock user repository
        Mockito.doReturn(lStoreUsers).when(storeRepository).getUserDetail(Mockito.any());
        
        // mock user product
        Mockito.doReturn(productTrxs).when(logRepository).getProductTrx(Mockito.any(), Mockito.any());

        // init unit test
        ResponseInfo<Object> responseInfo = orderUsecase.checkProduct(requestInfo, "123", "TRXID123");
        // assert
        assertEquals(HttpStatus.OK, responseInfo.getHttpStatus());
    }
    
}
