package com.java.project.msorder.config.jdbc;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class JdbcTemplateConfig {

    @Bean(name = "ms-db")
    @ConfigurationProperties(prefix = "spring.datasource-ms")
    public DataSource dataSource1() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "ms-jdbc-template")
    public JdbcTemplate jdbcTemplate1(@Qualifier("ms-db") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean(name = "transaction-db")
    @ConfigurationProperties(prefix = "spring.datasource-transaction")
    public DataSource dataSource2() {
        return  DataSourceBuilder.create().build();
    }

    @Bean(name = "transaction-jdbc")
    public JdbcTemplate jdbcTemplate2(@Qualifier("transaction-db") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
