package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.analytics.result.LowStockAlertResult;
import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryAnalyticsService {

    private final InventoryItemPersistencePort inventoryRepository;
    private final MerchantQueryPort merchantQueryPort;

    @Transactional(readOnly = true)
    public List<LowStockAlertResult> getMerchantLowStock(String ownerId) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new com.aionn.inventory.domain.exception.InventoryException(
                        com.aionn.inventory.domain.exception.InventoryErrorCode.WAREHOUSE_FORBIDDEN,
                        "No merchant registered for the authenticated user"));
        return inventoryRepository.findLowStockForMerchant(merchantId).stream()
                .map(row -> new LowStockAlertResult(
                        row.skuId(),
                        row.warehouseId(),
                        row.physicalQty(),
                        row.availableQty(),
                        row.safetyStockQty()))
                .toList();
    }
}
