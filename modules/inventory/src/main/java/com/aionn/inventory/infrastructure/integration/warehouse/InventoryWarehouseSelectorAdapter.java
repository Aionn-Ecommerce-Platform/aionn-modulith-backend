package com.aionn.inventory.infrastructure.integration.warehouse;

import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.contracts.port.inventory.WarehouseSelectorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InventoryWarehouseSelectorAdapter implements WarehouseSelectorPort {

    private final WarehousePersistencePort warehousePersistencePort;
    private final InventoryItemPersistencePort inventoryItemPersistencePort;

    @Override
    public Optional<String> selectWarehouseForSku(String merchantId, String skuId) {
        if (merchantId == null || merchantId.isBlank() || skuId == null || skuId.isBlank()) {
            return Optional.empty();
        }
        List<Warehouse> warehouses = warehousePersistencePort.findByMerchantOrderByPriority(merchantId).stream()
                .filter(w -> w.getStatus().canFulfill())
                .toList();
        if (warehouses.isEmpty()) {
            return Optional.empty();
        }
        List<String> warehouseIds = warehouses.stream().map(Warehouse::getWarehouseId).toList();
        List<InventoryItem> items = inventoryItemPersistencePort.findBySkuAcrossWarehouses(skuId, warehouseIds);
        for (Warehouse w : warehouses) {
            for (InventoryItem item : items) {
                if (item.getKey().warehouseId().equals(w.getWarehouseId())
                        && !item.isLocked()
                        && item.getAvailableQty() > 0) {
                    return Optional.of(w.getWarehouseId());
                }
            }
        }
        // No warehouse has stock — return empty so the caller reports
        // out-of-stock rather than reserving against a guaranteed-empty one.
        return Optional.empty();
    }
}
