package com.example.msinvoice.repository.impl;

import com.example.msinvoice.config.properties.AppProperties;
import com.example.msinvoice.models.repository.ProductTrx;
import com.example.msinvoice.models.repository.rowmapper.ProductTrxRowMapper;
import com.example.msinvoice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TransactionImplementationRepository implements TransactionRepository {
    @Autowired
    private AppProperties appProperties;
    private JdbcTemplate jdbcTemplate;

    public TransactionImplementationRepository(AppProperties appProperties, @Qualifier("transaction-jdbc")JdbcTemplate jdbcTemplate) {
        this.appProperties = appProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProductTrx> findProductTrx(String transactionId) {
        List<ProductTrx> productTrxes = new ArrayList<>();
        productTrxes = jdbcTemplate.query(appProperties.getQUERY_SELECT_PRODUCT_TRX_BY_ID(), new ProductTrxRowMapper(), transactionId);
        return productTrxes;
    }

    public void updateProductTrx(String transactionId , String orderStatus , String paymentStatus) {
        jdbcTemplate.update(appProperties.getQUERY_UPDATE_PRODUCT_STATUS_TRX_BY_ID(), orderStatus, paymentStatus, transactionId);
    }
}
