package com.aionn.ordering.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ordering.auto-cancel")
public record OrderingAutoCancelProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("15") int timeoutMinutes,
        @DefaultValue("60000") long delayMs,
        @DefaultValue("100") int batchSize) {
}
