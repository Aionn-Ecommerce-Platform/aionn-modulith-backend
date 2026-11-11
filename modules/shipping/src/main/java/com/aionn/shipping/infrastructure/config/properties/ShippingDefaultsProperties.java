package com.aionn.shipping.infrastructure.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "shipping.defaults")
public record ShippingDefaultsProperties(
        @DefaultValue("VND") String currency,
        @Min(1) @DefaultValue("1000") int baseWeightGram,
        @Min(1) @DefaultValue("500") int stepWeightGram) {
}
