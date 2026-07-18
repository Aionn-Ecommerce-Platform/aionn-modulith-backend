package com.aionn.ordering.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ordering")
public record OrderingProperties(
                @NotNull @Valid @DefaultValue Reservation reservation,
                @NotNull @Valid @DefaultValue AutoCancel autoCancel) {

        public record Reservation(
                        @Min(60) @Max(7 * 24 * 3600) @DefaultValue("86400") int ttlSeconds) {
        }

        public record AutoCancel(
                        @DefaultValue("true") boolean enabled,
                        @Min(1) @Max(1440) @DefaultValue("15") int timeoutMinutes,
                        @Min(1000) @Max(3_600_000) @DefaultValue("60000") long delayMs,
                        @Min(1) @Max(1000) @DefaultValue("100") int batchSize) {
        }
}
