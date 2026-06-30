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
            boolean active) {
    }
}
