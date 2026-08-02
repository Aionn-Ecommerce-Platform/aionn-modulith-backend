package com.aionn.catalog.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "catalog.cache.category-tree")
public record CategoryTreeCacheProperties(
        @DefaultValue("60") long l1TtlSeconds,
        @DefaultValue("10") long l1MaxSize,
        @DefaultValue("300") long l2TtlSeconds) {
}
