package com.aionn.ordering.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ordering.payment")
public record OrderingPaymentProperties(
        @DefaultValue("assume-success") String provider) {
}
