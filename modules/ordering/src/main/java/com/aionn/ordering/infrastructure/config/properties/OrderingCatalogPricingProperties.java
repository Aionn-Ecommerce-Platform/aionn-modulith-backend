package com.aionn.ordering.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "ordering.catalog-pricing")
public record OrderingCatalogPricingProperties(
        @DefaultValue("assume-available") String provider,
        @DefaultValue("test-merchant") String fallbackMerchantId,
        @DefaultValue("test-warehouse") String fallbackWarehouseId,
        @DefaultValue("99000") BigDecimal fallbackPrice,
        @DefaultValue("VND") String fallbackCurrency) {
}
