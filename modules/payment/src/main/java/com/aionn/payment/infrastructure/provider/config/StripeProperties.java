package com.aionn.payment.infrastructure.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.provider.stripe")
public record StripeProperties(
                String apiKey,
                String webhookSecret) {
}
