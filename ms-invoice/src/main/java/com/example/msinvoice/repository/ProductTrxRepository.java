package com.example.msinvoice.repository;

import com.example.msinvoice.model.repository.ProductTrx;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductTrxRepository extends JpaRepository<ProductTrx, Long> {

    Optional<ProductTrx> findByTransactionId(String transactionId);
}
