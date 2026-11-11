package com.aionn.ordering.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Resolves SKU price/merchant/active + warehouse to draw stock from at order
 * placement.
 */
public interface CatalogPricingGateway {

    Map<String, SkuPricing> resolve(List<String> skuIds);

    record SkuPricing(
            String skuId,
            String merchantId,
            String warehouseId,
            BigDecimal price,
            String currency,
            boolean active,
            List<String> categoryIds,
            String flashSaleRegistrationId,
            Parcel parcel,
            String productName,
            String imageUrl,
            String variantLabel) {
        public SkuPricing(String skuId, String merchantId, String warehouseId,
                BigDecimal price, String currency, boolean active) {
            this(skuId, merchantId, warehouseId, price, currency, active, List.of(), null, null,
                    null, null, null);
        }

        public SkuPricing(String skuId, String merchantId, String warehouseId,
                BigDecimal price, String currency, boolean active, List<String> categoryIds) {
            this(skuId, merchantId, warehouseId, price, currency, active, categoryIds, null, null,
                    null, null, null);
        }

        public SkuPricing(String skuId, String merchantId, String warehouseId,
                BigDecimal price, String currency, boolean active, List<String> categoryIds,
                String flashSaleRegistrationId) {
            this(skuId, merchantId, warehouseId, price, currency, active, categoryIds,
                    flashSaleRegistrationId, null, null, null, null);
        }

        public SkuPricing(String skuId, String merchantId, String warehouseId,
                BigDecimal price, String currency, boolean active, List<String> categoryIds,
                String flashSaleRegistrationId, Parcel parcel) {
            this(skuId, merchantId, warehouseId, price, currency, active, categoryIds,
                    flashSaleRegistrationId, parcel, null, null, null);
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
