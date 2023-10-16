package com.example.mspayment.repository.impl;

import com.example.mspayment.config.properties.AppProperties;
import com.example.mspayment.exception.definition.CommonException;
import com.example.mspayment.model.repository.ProductTrx;
import com.example.mspayment.model.repository.rowmapper.ProductTrxRowMapper;
import com.example.mspayment.model.rqrs.request.RequestInfo;
import com.example.mspayment.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TransactionImplementation implements TransactionRepository {
    @Autowired
    private AppProperties appProperties;
    private JdbcTemplate jdbcTemplate;

    public TransactionImplementation(AppProperties appProperties, @Qualifier("transaction-jdbc")JdbcTemplate jdbcTemplate) {
        this.appProperties = appProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProductTrx> findProductTrx(String transactionId) {
        List<ProductTrx> productTrxes = new ArrayList<>();
        productTrxes = jdbcTemplate.query(appProperties.getQUERY_SELECT_PRODUCT_TRX_BY_ID(), new ProductTrxRowMapper(), transactionId);
        return productTrxes;
    }

    @Override
    public void updateProductTrx(String transactionId , String orderStatus , String paymentStatus) {
        jdbcTemplate.update(appProperties.getQUERY_UPDATE_PRODUCT_STATUS_TRX_BY_ID(), orderStatus, paymentStatus, transactionId);
    }

}
