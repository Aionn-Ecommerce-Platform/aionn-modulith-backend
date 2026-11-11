package com.aionn.ordering.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ordering.analytics")
public record OrderingAnalyticsProperties(
        @DefaultValue("Asia/Ho_Chi_Minh") String zone,
        @Min(1) @Max(365) @DefaultValue("7") int merchantDefaultRangeDays,
        @Min(1) @Max(365) @DefaultValue("30") int platformDefaultRangeDays,
        @Min(1) @Max(100) @DefaultValue("10") int topMerchantsLimit,
        @Min(1) @Max(1000) @DefaultValue("20") int maxTopProductsLimit) {
}
