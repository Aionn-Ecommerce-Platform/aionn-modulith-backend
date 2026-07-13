package com.aionn.inventory.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "inventory")
public record InventoryProperties(@NotNull @Valid @DefaultValue Reservation reservation) {

    public record Reservation(@NotNull @Valid @DefaultValue AutoRelease autoRelease) {

        public record AutoRelease(
                @DefaultValue("true") boolean enabled,
                @Min(1000) @Max(3_600_000) @DefaultValue("30000") long delayMs,
                @Min(1) @Max(1000) @DefaultValue("100") int batchSize) {
        }
    }
}
