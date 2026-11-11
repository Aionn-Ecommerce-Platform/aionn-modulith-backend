package com.aionn.platform.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "aionn.shared.cache.two-tier")
public record TwoTierCacheSettings(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("") String origin) {
}
