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
            List<String> categoryIds,
            Parcel parcel) {
        public SkuPricing(String skuId, String merchantId, BigDecimal price, String currency, boolean active) {
            this(skuId, merchantId, price, currency, active, List.of(), null);
        }

        public SkuPricing(String skuId, String merchantId, BigDecimal price, String currency,
                boolean active, List<String> categoryIds) {
            this(skuId, merchantId, price, currency, active, categoryIds, null);
        }

        public SkuPricing {
            categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
        }

        /** Parcel characteristics for a sellable unit; weightGram required at publish. */
        public record Parcel(
                Integer weightGram,
                BigDecimal lengthCm,
                BigDecimal widthCm,
                BigDecimal heightCm) {
            public boolean hasWeight() {
                return weightGram != null && weightGram > 0;
            }
        }
    }
}
