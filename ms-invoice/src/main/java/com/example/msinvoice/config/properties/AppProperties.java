package com.example.msinvoice.config.properties;

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

    private String APPLICATION_NAME = "ms-logger";

    // kafka
    private String PAYMENTEVENT_KAFKA_TOPIC = "ms-notify-payment";
    private String PAYMENTEVENT_KAFKA_GROUPID = "ms-notify-payment-group";

    private String SERVICELOG_KAFKA_TOPIC = "ms-event-log";
    private String SERVICELOG_KAFKA_GROUPID = "event-service-log-group";

    private boolean PUBLISH_LOG_KAFKA = true;
}
