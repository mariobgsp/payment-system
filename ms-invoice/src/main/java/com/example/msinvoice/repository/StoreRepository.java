package com.example.msinvoice.repository;

import com.example.msinvoice.models.repository.Product;
import com.example.msinvoice.models.repository.StoreUser;

import java.util.List;

public interface StoreRepository {

    List<Product> getAllProduct();
    List<Product> getAvailableProduct();
    List<StoreUser> getUserDetail(String userName);
    List<Product> getSingleProduct(String productCode);
    List<Product> getSpecialProduct(boolean spFlag);
}
