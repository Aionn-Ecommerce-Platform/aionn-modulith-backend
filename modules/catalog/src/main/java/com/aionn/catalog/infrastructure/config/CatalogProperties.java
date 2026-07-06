package com.aionn.catalog.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "catalog")
public record CatalogProperties(
                @NotNull @Valid @DefaultValue MerchantSearchSync merchantSearchSync) {

        public record MerchantSearchSync(
                        @Min(1) @Max(100) @DefaultValue("100") int pageSize) {
        }
}
