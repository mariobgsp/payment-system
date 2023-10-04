package com.example.mspayment.repository;

import com.example.mspayment.model.repository.ProductTrx;
import com.example.mspayment.model.rqrs.request.RequestInfo;

import java.util.List;

public interface TransactionRepository {

    List<ProductTrx> findProductTrx(String transactionId);
    void updateProductTrx(String transactionId, String orderStatus, String paymentStatus);
}
