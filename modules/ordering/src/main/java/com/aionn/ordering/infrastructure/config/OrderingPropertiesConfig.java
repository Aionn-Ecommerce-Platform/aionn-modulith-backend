package com.aionn.ordering.infrastructure.config;

import com.aionn.ordering.infrastructure.config.properties.OrderingAnalyticsProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingAutoCancelProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingCompensationProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingDefaultsProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingReservationProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingReturnProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        OrderingReservationProperties.class,
        OrderingReturnProperties.class,
        OrderingAutoCancelProperties.class,
        OrderingDefaultsProperties.class,
        OrderingCompensationProperties.class,
        OrderingAnalyticsProperties.class
})
public class OrderingPropertiesConfig {
}
