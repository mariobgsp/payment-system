package com.java.project.msorder.repository;

import com.java.project.msorder.config.properties.AppProperties;
import com.java.project.msorder.exception.definition.CommonException;
import com.java.project.msorder.model.repository.ProductTrx;
import com.java.project.msorder.model.repository.StoreUser;
import com.java.project.msorder.model.rqrs.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductTrxRepository {
    @Autowired
    private AppProperties appProperties;
    private NamedParameterJdbcTemplate jdbcTemplate;

    public ProductTrxRepository(AppProperties appProperties, @Qualifier("transaction-jdbc")NamedParameterJdbcTemplate jdbcTemplate) {
        this.appProperties = appProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    RowMapper<ProductTrx> rowMapperProductTrx = (rs, rowNum) ->{
        ProductTrx productTrx = new ProductTrx();
        productTrx.setId(rs.getString("id"));
        productTrx.setSysCreationDate(rs.getString("sys_creation_date"));
        productTrx.setSysUpdateDate(rs.getString("transactionid"));
        productTrx.setOrderStatus(rs.getString("orderstatus"));
        productTrx.setPaymentStatus(rs.getString("paymentstatus"));
        productTrx.setUserId(rs.getString("userid"));
        productTrx.setProductName(rs.getString("productname"));
        productTrx.setAmount(Long.valueOf(rs.getString("amount")));
        productTrx.setPrice(Long.valueOf(rs.getString("price")));
        productTrx.setPriceCharge(Long.valueOf(rs.getString("pricecharge")));
        productTrx.setProductCode(rs.getString("productcode"));
        productTrx.setParam_1(rs.getString("param_1"));
        productTrx.setParam_2(rs.getString("param_2"));
        productTrx.setSysUpdateDate(rs.getString("sys_update_date"));
        productTrx.setPaymentDate(rs.getString("payment_date"));
        productTrx.setDiscountEnabled(Boolean.parseBoolean(rs.getString("discount_enabled")));
        productTrx.setDiscount(Double.valueOf(rs.getString("discount")));
        return productTrx;
    };


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
            MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("transactionid", transactionId);

            productTrxes = jdbcTemplate.query(appProperties.getQUERY_GET_PRODUCT_TRX_BY_TRANSACTIONID(), parameterSource, rowMapperProductTrx);
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
