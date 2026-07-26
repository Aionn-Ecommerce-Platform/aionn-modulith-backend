package com.aionn.shipping.infrastructure.config;

import com.aionn.shipping.infrastructure.config.properties.ShippingCarrierProperties;
import com.aionn.shipping.infrastructure.config.properties.ShippingDefaultsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        ShippingCarrierProperties.class,
        ShippingDefaultsProperties.class
})
public class LegacyShippingPropertiesConfig {
}
