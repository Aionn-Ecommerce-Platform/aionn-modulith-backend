package com.aionn.ordering.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ordering.return")
public record OrderingReturnProperties(
        @Min(1) @Max(365) @DefaultValue("7") int windowDays,
        @Min(1) @Max(20) @DefaultValue("5") int refundMaxAttempts,
        @Min(1) @Max(1000) @DefaultValue("100") int refundBatchSize) {

    @ConstructorBinding
    public OrderingReturnProperties {
    }

    public OrderingReturnProperties(int windowDays) {
        this(windowDays, 5, 100);
    }
}
