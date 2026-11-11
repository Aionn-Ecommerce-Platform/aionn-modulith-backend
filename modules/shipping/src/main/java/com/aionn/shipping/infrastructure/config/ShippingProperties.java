package com.aionn.shipping.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "shipping")
public record ShippingProperties(
        @NotNull @Valid @DefaultValue StatusPoller statusPoller) {

    public record StatusPoller(
            @DefaultValue("true") boolean enabled,
            @Min(1000) @Max(3_600_000) @DefaultValue("60000") long delayMs,
            @Min(1) @Max(1000) @DefaultValue("100") int batchSize) {
    }
}
