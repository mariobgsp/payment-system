package com.example.mslogger.config.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Configuration
public class AppProperties {

    private String APPLICATION_NAME = "ms-order";

    private String SECRET_KEY = "bXlvbmx5X3Bhc3N3b3JkOg==";
    private String ORDER_STATUS_CREATED = "CREATED";
    private String PAYMENT_STATUS_CREATED = "CREATED";

    // mongodb client
    private String MONGODB_URI = "mongodb://admin:password@localhost:27017";
    private String MONGODB_DATABASE_NAME = "servicelog";

}
