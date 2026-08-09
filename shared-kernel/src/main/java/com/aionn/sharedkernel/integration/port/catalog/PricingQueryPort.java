package com.aionn.sharedkernel.integration.port.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PricingQueryPort {

    Map<String, SkuPricing> resolvePricing(List<String> skuIds);

    record SkuPricing(
            String skuId,
            String merchantId,
            BigDecimal price,
            String currency,
            boolean active,
            List<String> categoryIds) {
        public SkuPricing(String skuId, String merchantId, BigDecimal price, String currency, boolean active) {
            this(skuId, merchantId, price, currency, active, List.of());
        }

        public SkuPricing {
            categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
        }
    }
}
