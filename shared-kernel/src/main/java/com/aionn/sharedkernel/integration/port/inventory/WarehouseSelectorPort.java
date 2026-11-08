package com.aionn.sharedkernel.integration.port.inventory;

import java.util.Optional;

// Picks the warehouse a SKU ships from during checkout. Inventory owns the decision (it reads priority
// and on-hand stock); ordering must not touch inventory tables.
public interface WarehouseSelectorPort {

    // Highest-priority warehouse with stock, else the highest-priority one (backorder address), else
    // empty when the merchant has no warehouses at all.
    Optional<String> selectWarehouseForSku(String merchantId, String skuId);
}
