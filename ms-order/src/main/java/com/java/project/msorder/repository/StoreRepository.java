package com.java.project.msorder.repository;

import com.java.project.msorder.model.repository.Product;
import com.java.project.msorder.model.repository.StoreUser;
import com.java.project.msorder.repository.impl.StoreImplementationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StoreRepository implements StoreImplementationRepository {

    private JdbcTemplate jdbcTemplate;

    public StoreRepository(@Qualifier("ms-jdbc-template")JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    RowMapper<Product> rowMapper = (rs, rowNum) ->{
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
    };

    RowMapper<StoreUser> rowMapperUser = (rs, rowNum) ->{
        StoreUser storeUser = new StoreUser();
        storeUser.setId(rs.getInt("ID"));
        storeUser.setUserId(rs.getString("USERID"));
        storeUser.setUserName(rs.getString("USERNAME"));
        storeUser.setFirstName(rs.getString("FIRSTNAME"));
        storeUser.setLastName(rs.getString("LASTNAME"));
        storeUser.setEmail(rs.getString("EMAIL"));
        storeUser.setHashPassword(rs.getString("PASSWORD"));
        storeUser.setSpecialProduct(Boolean.parseBoolean(rs.getString("SPECIALPRODUCT")));
        storeUser.setRecurring(Boolean.parseBoolean(rs.getString("RECURRING")));
        storeUser.setToken(rs.getString("TOKEN"));
        return storeUser;
    };

    @Override
    public List<Product> getAllProduct() {
        String sql = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT ";
        return jdbcTemplate.query(sql, rowMapper);
    }
    @Override
    public List<Product> getAvailableProduct() {
        String sql = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT WHERE PRODUCTSTATUS = true ";
        return jdbcTemplate.query(sql, rowMapper);
    }
    @Override
    public List<StoreUser> getUserDetail(String userName){
        String sql = "SELECT ID, USERID, USERNAME, FIRSTNAME, LASTNAME, EMAIL, PASSWORD, SPECIALPRODUCT, RECURRING, TOKEN FROM STORE.STORE_USER WHERE USERNAME = ?";
        return jdbcTemplate.query(sql, rowMapperUser, userName);
    }
    @Override
    public List<Product> getSingleProduct(String productCode) {
        String sql = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT WHERE PRODUCTCODE = ? AND PRODUCTSTATUS is true ";
        return jdbcTemplate.query(sql, rowMapper, productCode);
    }

    @Override
    public List<Product> getSpecialProduct(boolean spFlag){
        String sql = "SELECT PRODUCTID, PRODUCTSTATUS, PRODUCTCODE, PRODUCTNAME, PRICE, DISCOUNT, ENABLEDISCOUNT, SPECIALPRODUCT, PRODUCTUPDATEDATE, PRODUCTINSERTDATE FROM STORE.PRODUCT WHERE SPECIALPRODUCT is {boolean} AND PRODUCTSTATUS is true ".replace("{boolean}", String.valueOf(spFlag));
        return jdbcTemplate.query(sql, rowMapper);
    }
}
