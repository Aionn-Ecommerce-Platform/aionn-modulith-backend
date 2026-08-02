package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "identity.cache.geography")
public record GeographyCacheProperties(
        @DefaultValue("1800") long l1TtlSeconds,
        @DefaultValue("5000") long l1MaxSize,
        @DefaultValue("86400") long l2TtlSeconds) {
}
