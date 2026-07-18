package com.aionn.ordering.infrastructure.config;

import com.aionn.ordering.infrastructure.config.properties.OrderingAutoCancelProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingCatalogPricingProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingDefaultsProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingPaymentProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingReservationProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingReturnProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingShippingProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingVoucherProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        OrderingPaymentProperties.class,
        OrderingShippingProperties.class,
        OrderingCatalogPricingProperties.class,
        OrderingVoucherProperties.class,
        OrderingReservationProperties.class,
        OrderingReturnProperties.class,
        OrderingAutoCancelProperties.class,
        OrderingDefaultsProperties.class
})
public class OrderingPropertiesConfig {
}
