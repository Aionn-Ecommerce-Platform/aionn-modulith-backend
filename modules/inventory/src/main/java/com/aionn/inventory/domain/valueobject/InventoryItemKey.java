package com.aionn.inventory.domain.valueobject;

import java.util.Objects;

/**
 * Composite key (skuId + warehouseId) used to address an InventoryItem.
 * Implemented as a record so JPA composite-id mappings can keep the same
 * shape without a hand-rolled equals/hashCode.
 */
public record InventoryItemKey(String skuId, String warehouseId) {
    public InventoryItemKey {
        Objects.requireNonNull(skuId, "skuId");
        Objects.requireNonNull(warehouseId, "warehouseId");
    }
}

