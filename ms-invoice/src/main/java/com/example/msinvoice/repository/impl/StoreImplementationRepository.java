package com.example.msinvoice.repository.impl;

import com.example.msinvoice.config.properties.AppProperties;
import com.example.msinvoice.models.repository.Product;
import com.example.msinvoice.models.repository.StoreUser;
import com.example.msinvoice.models.repository.rowmapper.ProductRowMapper;
import com.example.msinvoice.models.repository.rowmapper.StoreUserRowMapper;
import com.example.msinvoice.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StoreImplementationRepository implements StoreRepository {

    private AppProperties appProperties;
    private JdbcTemplate jdbcTemplate;

    public StoreImplementationRepository(AppProperties appProperties, @Qualifier("ms-jdbc-template")JdbcTemplate jdbcTemplate) {
        this.appProperties = appProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Product> getAllProduct() {
        return jdbcTemplate.query(appProperties.getQUERY_GET_ALL_PRODUCT(), new ProductRowMapper());
    }
    @Override
    public List<Product> getAvailableProduct() {
        return jdbcTemplate.query(appProperties.getQUERY_GET_AVAILABLE_PRODUCT(), new ProductRowMapper());
    }
    @Override
    public List<StoreUser> getUserDetail(String userName){
        return jdbcTemplate.query(appProperties.getQUERY_GET_USER_DETAIL(), new StoreUserRowMapper(), userName);
    }
    @Override
    public List<Product> getSingleProduct(String productCode) {
        return jdbcTemplate.query(appProperties.getQUERY_GET_SINGLE_PRODUCT(), new ProductRowMapper(), productCode);
    }

    @Override
    public List<Product> getSpecialProduct(boolean spFlag){
        return jdbcTemplate.query(appProperties.getQUERY_GET_SPECIFIED_PRODUCT().replace("{boolean}", String.valueOf(spFlag)), new ProductRowMapper());
    }
}
