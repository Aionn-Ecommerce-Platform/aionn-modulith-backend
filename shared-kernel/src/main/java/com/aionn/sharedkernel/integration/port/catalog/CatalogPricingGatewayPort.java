package com.aionn.sharedkernel.integration.port.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Outbound port for resolving SKU pricing from the Catalog module.
 *
 * <p>
 * Used synchronously by the Ordering module during checkout. Must return
 * immediately so the order can be priced/validated within the same
 * transaction.
 * </p>
 */
public interface CatalogPricingGatewayPort {

    PricingResult resolve(List<String> skuIds);

    record PricingResult(List<SkuPricing> items) {

        public PricingResult {
            items = List.copyOf(items);
        }

        public Optional<SkuPricing> findBySku(String skuId) {
            return items.stream()
                    .filter(item -> item.skuId().equals(skuId))
                    .findFirst();
        }

        public Map<String, SkuPricing> asMap() {
            return items.stream()
                    .collect(Collectors.toUnmodifiableMap(SkuPricing::skuId, Function.identity()));
        }
    }

    record SkuPricing(
            String skuId,
            String merchantId,
            String warehouseId,
            BigDecimal price,
            String currency,
            boolean active) {
    }
}
