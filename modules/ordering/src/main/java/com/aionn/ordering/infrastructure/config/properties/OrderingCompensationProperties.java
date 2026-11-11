package com.aionn.ordering.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ordering.compensation")
public record OrderingCompensationProperties(
        @Min(1) @Max(100) @DefaultValue("10") int maxAttempts,
        @Min(1) @Max(1000) @DefaultValue("100") int batchSize) {
}
