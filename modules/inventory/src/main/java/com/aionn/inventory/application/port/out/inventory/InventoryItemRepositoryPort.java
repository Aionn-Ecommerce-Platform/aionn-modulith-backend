package com.aionn.inventory.application.port.out.inventory;

import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepositoryPort {

    InventoryItem save(InventoryItem item);

    Optional<InventoryItem> findByKey(InventoryItemKey key);

    Optional<InventoryItem> lockByKey(InventoryItemKey key);

    List<InventoryItem> findBySkuAcrossWarehouses(String skuId, List<String> warehouseIds);

    List<InventoryItem> findBySku(String skuId);

    List<InventoryItem> findByWarehouse(String warehouseId, int page, int size);

    List<InventoryItem> findLowStock(String merchantId, int page, int size);
}
