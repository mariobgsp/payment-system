package com.example.msinvoice.repository;

import com.example.msinvoice.models.repository.ProductTrx;

import java.util.List;

public interface TransactionRepository {

    List<ProductTrx> findProductTrx(String transactionId);
    void updateProductTrx(String transactionId, String orderStatus, String paymentStatus);
}
