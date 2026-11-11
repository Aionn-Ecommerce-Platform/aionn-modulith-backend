package com.aionn.ucp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "aionn.ucp.webhook")
public record UcpWebhookProperties(
        @DefaultValue("5000") long pollDelayMs,
        @DefaultValue("50") int batchSize,
        @DefaultValue("10") int maxAttempts) {
}
