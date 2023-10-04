package com.example.mspayment.model.repository.rowmapper;

import com.example.mspayment.model.repository.ProductTrx;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductTrxRowMapper implements RowMapper<ProductTrx> {
    @Override
    public ProductTrx mapRow(ResultSet rs , int rowNum) throws SQLException {
        ProductTrx productTrx = new ProductTrx();
        productTrx.setId(rs.getString("id"));
        productTrx.setTransactionId(rs.getString("transactionid"));
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
        productTrx.setDiscount(rs.getDouble("discount"));
        return productTrx;
    }
}
