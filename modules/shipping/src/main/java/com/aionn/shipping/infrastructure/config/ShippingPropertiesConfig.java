package com.aionn.shipping.infrastructure.config;

import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GhnProperties.class)
public class ShippingPropertiesConfig {
}
