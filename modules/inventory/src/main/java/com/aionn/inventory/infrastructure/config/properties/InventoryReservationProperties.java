package com.aionn.inventory.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "inventory.reservation")
public record InventoryReservationProperties(
        @DefaultValue("900") int defaultTtlSeconds,
        @DefaultValue("100") int autoReleaseBatchSize,
        @DefaultValue AutoRelease autoRelease) {

    public record AutoRelease(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("30000") long delayMs,
            @DefaultValue("100") int batchSize) {
    }
}
