package com.java.project.msorder.repository;

import com.java.project.msorder.model.repository.Product;
import com.java.project.msorder.repository.impl.StoreImplementationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StoreRepository implements StoreImplementationRepository {

    private JdbcTemplate jdbcTemplate;


    public StoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    RowMapper<Product> rowMapper = (rs, rowNum) ->{
        Product product = new Product();
        product.setProductId(rs.getInt("PRODUCTID"));
        product.setProductStatus(Boolean.parseBoolean(rs.getString("PRODUCTSTATUS")));
        product.setProductCode(rs.getString("PRODUCTCODE"));
        product.setProductName(rs.getString("PRODUCTNAME"));
        product.setPrice(rs.getInt("PRICE"));
        product.setDiscount(rs.getDouble("DISCOUNT"));
        product.setEnableDiscount(Boolean.parseBoolean(rs.getString("ENABLEDISCOUNT")));
        product.setSpecialProductLabel(Boolean.parseBoolean(rs.getString("SPECIALPRODUCT")));
        product.setProductUpdate(rs.getString("PRODUCTUPDATEDATE"));
        product.setProductInsert(rs.getString("PRODUCTINSERTDATE"));
        return product;
    };

    @Override
    public List<Product> getAllProduct() {
        String sql = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT ";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<Product> findProductByCode(String productCode) {
        return null;
    }


}
