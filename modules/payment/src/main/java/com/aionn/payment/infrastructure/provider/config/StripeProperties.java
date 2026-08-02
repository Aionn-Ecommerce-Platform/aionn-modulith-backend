package com.aionn.payment.infrastructure.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "payment.provider.stripe")
public record StripeProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String apiKey,
        @DefaultValue("") String webhookSecret) {
}
