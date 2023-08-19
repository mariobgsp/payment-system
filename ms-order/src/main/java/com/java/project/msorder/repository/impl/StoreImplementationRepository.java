package com.java.project.msorder.repository.impl;

import com.java.project.msorder.model.repository.Product;
import com.java.project.msorder.model.repository.StoreUser;

import java.util.List;

public interface StoreImplementationRepository {

    List<Product> getAllProduct();
    List<Product> getAvailableProduct();
    List<StoreUser> getUserDetail(String userName);
    List<Product> getSingleProduct(String productCode);
    List<Product> getSpecialProduct(boolean spFlag);
}
