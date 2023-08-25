package com.example.msorder.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.msorder.config.properties.AppProperties;
import com.example.msorder.exception.definition.CommonException;
import com.example.msorder.model.repository.ProductTrx;
import com.example.msorder.model.repository.rowmapper.ProductTrxRowMapper;
import com.example.msorder.model.rqrs.request.RequestInfo;

import java.util.ArrayList;
import java.util.List;

@Repository
public class LogRepository {
    @Autowired
    private AppProperties appProperties;
    private NamedParameterJdbcTemplate jdbcTemplate;

    public LogRepository(AppProperties appProperties, @Qualifier("transaction-jdbc")NamedParameterJdbcTemplate jdbcTemplate) {
        this.appProperties = appProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertProductTrx(RequestInfo requestInfo, ProductTrx productTrx) throws CommonException {
        try {

            MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("id", productTrx.getId());
            parameterSource.addValue("transactionid", productTrx.getTransactionId());
            parameterSource.addValue("orderstatus", productTrx.getOrderStatus());
            parameterSource.addValue("paymentstatus", productTrx.getPaymentStatus());
            parameterSource.addValue("userid", productTrx.getUserId());
            parameterSource.addValue("productname", productTrx.getProductName());
            parameterSource.addValue("amount", productTrx.getAmount());
            parameterSource.addValue("price", productTrx.getPrice());
            parameterSource.addValue("pricecharge", productTrx.getPriceCharge());
            parameterSource.addValue("productcode", productTrx.getProductCode());
            parameterSource.addValue("param1", productTrx.getParam_1());
            parameterSource.addValue("param2", productTrx.getParam_2());
            parameterSource.addValue("discEnabled", productTrx.isDiscountEnabled());
            parameterSource.addValue("discount", productTrx.getDiscount());

            jdbcTemplate.update(appProperties.getQUERY_INSERT_PRODUCT_TRX(), parameterSource);
        }catch (Exception e){
            throw new CommonException(e);
        }

    }

    public List<ProductTrx> getProductTrx(RequestInfo requestInfo, String transactionId) throws CommonException {
        List<ProductTrx> productTrxes = new ArrayList<>();

        try{
            String sql = "SELECT id, sys_creation_date, transactionid, orderstatus, paymentstatus, userid, productname, amount, price, pricecharge, productcode, param_1, param_2, sys_update_date, payment_date, discount_enabled, discount FROM transaction.product_trx where transactionid = :trxid ";

            MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("trxid", transactionId);

            productTrxes = jdbcTemplate.query(sql, parameterSource, new ProductTrxRowMapper());
        }catch (Exception e){
            throw new CommonException(e);
        }
        return productTrxes;
    }

    public Long getSequence(RequestInfo requestInfo) throws CommonException {
        Long seq = 0L;
        String sql = "select nextval('transaction.product_trx_seq') as sequence";
        try {
            MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            seq = jdbcTemplate.queryForObject(sql , parameterSource , Long.class);
        } catch (Exception e) {
            throw new CommonException(e);
        }
        return seq;
    }
}
