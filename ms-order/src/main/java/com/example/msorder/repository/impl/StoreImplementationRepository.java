package com.example.msorder.repository.impl;

import java.util.List;

import com.example.msorder.model.repository.Product;
import com.example.msorder.model.repository.StoreUser;

public interface StoreImplementationRepository {

    List<Product> getAllProduct();
    List<Product> getAvailableProduct();
    List<StoreUser> getUserDetail(String userName);
    List<Product> getSingleProduct(String productCode);
    List<Product> getSpecialProduct(boolean spFlag);
}
