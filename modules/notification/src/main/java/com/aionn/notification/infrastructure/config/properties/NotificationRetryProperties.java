package com.aionn.notification.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notification.retry")
public record NotificationRetryProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("30000") long delayMs,
        @DefaultValue("100") int batchSize) {
}
