package com.example.mspayment.config.jdbc;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class JdbcTemplateConfig {

    @Bean(name = "transaction-db")
    @ConfigurationProperties(prefix = "spring.datasource-transaction")
    public DataSource dataSource() {
        return  DataSourceBuilder.create().build();
    }

    @Bean(name = "transaction-jdbc")
    public JdbcTemplate jdbcTemplate(@Qualifier("transaction-db") DataSource ds) {
        return new JdbcTemplate(ds);
    }


}
