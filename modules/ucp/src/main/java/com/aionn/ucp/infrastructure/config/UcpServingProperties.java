package com.aionn.ucp.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aionn.ucp.serving")
public record UcpServingProperties(
        @Min(1) @Max(3650) @DefaultValue("7") int cartExpiryDays,
        @Min(1) @Max(720) @DefaultValue("6") int checkoutTtlHours,
        @NotBlank @DefaultValue("https://aionn.vn") String storefrontBaseUrl,
        @Min(1) @Max(10_000) @DefaultValue("10") int catalogSearchDefaultLimit,
        @Min(1) @Max(10_000) @DefaultValue("100") int catalogSearchMaxLimit) {
}
