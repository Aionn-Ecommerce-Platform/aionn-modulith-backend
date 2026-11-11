package com.aionn.payment.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payment.analytics")
public record PaymentAnalyticsProperties(
        @DefaultValue("UTC") String zone,
        @Min(1) @Max(365) @DefaultValue("30") int defaultRangeDays,
        @Min(1) @Max(3650) @DefaultValue("366") long maxRangeDays) {
}
