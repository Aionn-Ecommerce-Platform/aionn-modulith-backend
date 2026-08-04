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
            List<String> categoryIds) {
        public SkuPricing(String skuId, String merchantId, String warehouseId,
                BigDecimal price, String currency, boolean active) {
            this(skuId, merchantId, warehouseId, price, currency, active, List.of());
        }

        public SkuPricing {
            categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
        }
    }
}
