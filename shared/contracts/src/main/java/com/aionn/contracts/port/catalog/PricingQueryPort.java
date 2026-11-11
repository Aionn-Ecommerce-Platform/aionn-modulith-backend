package com.aionn.contracts.port.catalog;

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
            Parcel parcel,
            String productName,
            String imageUrl,
            String variantLabel) {
        public SkuPricing(String skuId, String merchantId, BigDecimal price, String currency, boolean active) {
            this(skuId, merchantId, price, currency, active, List.of(), null, null, null, null);
        }

        public SkuPricing(String skuId, String merchantId, BigDecimal price, String currency,
                boolean active, List<String> categoryIds) {
            this(skuId, merchantId, price, currency, active, categoryIds, null, null, null, null);
        }

        public SkuPricing(String skuId, String merchantId, BigDecimal price, String currency,
                boolean active, List<String> categoryIds, Parcel parcel) {
            this(skuId, merchantId, price, currency, active, categoryIds, parcel, null, null, null);
        }

        public SkuPricing {
            categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
        }

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
