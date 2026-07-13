package com.aionn.inventory.application.port.out;

import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InventoryItemPersistencePort {

    InventoryItem save(InventoryItem item);

    Optional<InventoryItem> findByKey(InventoryItemKey key);

    Optional<InventoryItem> lockByKey(InventoryItemKey key);

    List<InventoryItem> findBySkuAcrossWarehouses(String skuId, List<String> warehouseIds);

    List<InventoryItem> findBySku(String skuId);

    Page<InventoryItem> findByWarehouse(String warehouseId, Pageable pageable);
}
