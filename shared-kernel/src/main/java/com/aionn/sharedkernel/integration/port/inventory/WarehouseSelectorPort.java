package com.aionn.sharedkernel.integration.port.inventory;

import java.util.Optional;

// Outbound port used by ordering during checkout to pick which warehouse a
// given SKU should ship from. Picking is inventory's responsibility because it
// needs to read warehouse priority + on-hand stock — ordering must not touch
// the inventory database directly.
// Returns empty when the merchant has no warehouses; ordering treats that as
// "no fulfillment route" and rejects the line.
public interface WarehouseSelectorPort {

    // Returns the warehouse with available stock, in priority order; or the
    // highest-priority warehouse when no warehouse has stock (so the caller can
    // still surface a backorder address); or empty when the merchant has no
    // warehouses configured at all.
    Optional<String> selectWarehouseForSku(String merchantId, String skuId);
}
