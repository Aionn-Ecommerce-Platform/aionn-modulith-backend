package com.aionn.inventory.application.port.out;

import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.valueobject.InventoryItemKey;
import com.aionn.inventory.application.dto.common.PageResult;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface InventoryItemPersistencePort {

    InventoryItem save(InventoryItem item);

    Optional<InventoryItem> findByKey(InventoryItemKey key);

    Optional<InventoryItem> lockByKey(InventoryItemKey key);

    InventoryItem createIfAbsentAndLock(InventoryItemKey key, Instant now);

    List<InventoryItem> findBySkuAcrossWarehouses(String skuId, List<String> warehouseIds);

    List<InventoryItem> findBySku(String skuId);

    PageResult<InventoryItem> findByWarehouse(String warehouseId, OffsetPagination pagination);

    List<LowStockItem> findLowStockForMerchant(String merchantId);

    record LowStockItem(
            String skuId,
            String warehouseId,
            int physicalQty,
            int availableQty,
            int safetyStockQty) {
    }
}
