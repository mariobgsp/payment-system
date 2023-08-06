package com.java.project.msorder.repository.impl;

import com.java.project.msorder.model.repository.Product;

import java.util.List;

public interface StoreImplementationRepository {

    List<Product> getAllProduct();
    List<Product> findProductByCode(String productCode);
}
