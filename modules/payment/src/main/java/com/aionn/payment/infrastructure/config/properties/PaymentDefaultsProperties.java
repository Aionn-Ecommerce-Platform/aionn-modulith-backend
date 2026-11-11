package com.aionn.payment.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "payment.defaults")
public record PaymentDefaultsProperties(
        @DefaultValue("VND") String currency) {
}
