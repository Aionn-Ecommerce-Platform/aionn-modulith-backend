package com.aionn.platform.infrastructure.outbox;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Centralised outbox tuning so every instance uses validated, externally configurable values. */
@Validated
@ConfigurationProperties(prefix = "aionn.outbox")
public record OutboxProperties(
        @Min(1) @DefaultValue("1000") long pollDelayMs,
        @Min(1) @DefaultValue("50") int batchSize,
        @Min(1) @DefaultValue("10") int maxAttempts,
        @Min(1) @DefaultValue("300") long leaseDurationSeconds) {
}
