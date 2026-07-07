package com.aionn.catalog.application.port.out.product;

import java.util.List;
import java.util.Map;

// Read/write surface for the per-product sold counter. Catalog persists the
// counter; ordering bumps it on completed orders. The storefront uses it to
// render the "sold X" badge on product cards.
public interface ProductSoldCounterPersistencePort {

    long getSoldCount(String productId);

    Map<String, Long> getSoldCountsByProductIds(List<String> productIds);

    void incrementSoldCount(String productId, long delta);
}
