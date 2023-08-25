package com.example.msorder.model.repository.rowmapper;

import org.springframework.jdbc.core.RowMapper;

import com.example.msorder.model.repository.Product;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductRowMapper implements RowMapper<Product> {
    @Override
    public Product mapRow(ResultSet rs , int rowNum) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("PRODUCTID"));
        product.setProductStatus(rs.getBoolean("PRODUCTSTATUS"));
        product.setProductCode(rs.getString("PRODUCTCODE"));
        product.setProductName(rs.getString("PRODUCTNAME"));
        product.setPrice(rs.getInt("PRICE"));
        product.setDiscount(rs.getDouble("DISCOUNT"));
        product.setEnableDiscount(rs.getBoolean("ENABLEDISCOUNT"));
        product.setSpecialProductLabel(rs.getBoolean("SPECIALPRODUCT"));
        product.setProductUpdate(rs.getString("PRODUCTUPDATEDATE"));
        product.setProductInsert(rs.getString("PRODUCTINSERTDATE"));
        return product;
    }
}
