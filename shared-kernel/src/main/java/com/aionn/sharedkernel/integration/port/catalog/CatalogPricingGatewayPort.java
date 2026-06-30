package com.aionn.sharedkernel.integration.port.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.List;

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
