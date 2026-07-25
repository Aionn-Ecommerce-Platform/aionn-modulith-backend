package com.aionn.shipping.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "shipping.defaults")
public record ShippingDefaultsProperties(
        @DefaultValue("VND") String currency) {
}
