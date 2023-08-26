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
import com.example.msorder.model.rqrs.response.ResponseInfo;
import com.example.msorder.repository.LogRepository;
import com.example.msorder.repository.StoreRepository;
import com.example.msorder.utils.CommonUtils;

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
    void orderProduct(){

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
