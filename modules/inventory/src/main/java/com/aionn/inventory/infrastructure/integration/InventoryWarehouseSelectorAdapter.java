package com.aionn.inventory.infrastructure.integration;

import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.sharedkernel.integration.port.inventory.WarehouseSelectorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Implements the cross-service WarehouseSelectorPort by inspecting inventory's
 * own warehouse and stock tables. The pick rule mirrors what ordering used to
 * do inline: highest-priority warehouse with available stock; otherwise fall
 * back to the highest-priority warehouse so the caller still has a routing
 * target.
 */
@Component
@RequiredArgsConstructor
public class InventoryWarehouseSelectorAdapter implements WarehouseSelectorPort {

    private final WarehousePersistencePort warehouseRepository;
    private final InventoryItemPersistencePort inventoryItemRepository;

    @Override
    public Optional<String> selectWarehouseForSku(String merchantId, String skuId) {
        if (merchantId == null || merchantId.isBlank() || skuId == null || skuId.isBlank()) {
            return Optional.empty();
        }
        List<Warehouse> warehouses = warehouseRepository.findByMerchantOrderByPriority(merchantId);
        if (warehouses.isEmpty()) {
            return Optional.empty();
        }
        List<String> warehouseIds = warehouses.stream().map(Warehouse::getWarehouseId).toList();
        List<InventoryItem> items = inventoryItemRepository.findBySkuAcrossWarehouses(skuId, warehouseIds);
        for (Warehouse w : warehouses) {
            for (InventoryItem item : items) {
                if (item.getKey().warehouseId().equals(w.getWarehouseId()) && item.getAvailableQty() > 0) {
                    return Optional.of(w.getWarehouseId());
                }
            }
        }
        return Optional.of(warehouses.get(0).getWarehouseId());
    }
}
