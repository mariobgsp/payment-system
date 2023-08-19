package com.java.project.msorder.repository;

import com.java.project.msorder.exception.definition.CommonException;
import com.java.project.msorder.model.repository.ProductTrx;
import com.java.project.msorder.model.rqrs.request.RequestInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductTrxRepository {

    private NamedParameterJdbcTemplate jdbcTemplate;

    public ProductTrxRepository(@Qualifier("transaction-jdbc")NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertProductTrx(RequestInfo requestInfo, ProductTrx productTrx) throws CommonException {
        try {
            String sql = "INSERT INTO transaction.product_trx(id, sys_creation_date, transactionid, userid, productname, amount, price, pricecharge, productcode, param_1, param_2, sys_update_date, payment_date, discount_enabled, discount) " +
                    " VALUES (:id, localtimestamp, :transactionid, :userid , :productname, :amount, :price, :pricecharge, :productcode, :param1, :param2, null, null, :discEnabled, :discount)";

            MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("id", productTrx.getId());
            parameterSource.addValue("transactionid", productTrx.getTransactionId());
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

            jdbcTemplate.update(sql, parameterSource);
        }catch (Exception e){
            throw new CommonException(e);
        }

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
