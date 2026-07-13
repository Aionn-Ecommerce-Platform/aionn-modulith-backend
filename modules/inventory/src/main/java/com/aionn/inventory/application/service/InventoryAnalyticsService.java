package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.analytics.result.LowStockAlertResult;
import com.aionn.inventory.infrastructure.persistence.repository.InventoryItemRepository;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryAnalyticsService {

    private final InventoryItemRepository inventoryRepository;
    private final MerchantQueryPort merchantQueryPort;

    @Transactional(readOnly = true)
    public List<LowStockAlertResult> getMerchantLowStock(String ownerId) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new IllegalStateException("No merchant for owner " + ownerId));
        return inventoryRepository.findLowStockForMerchant(merchantId).stream()
                .map(row -> new LowStockAlertResult(
                        row.getSkuId(),
                        row.getWarehouseId(),
                        row.getPhysicalQty() == null ? 0 : row.getPhysicalQty(),
                        row.getAvailableQty() == null ? 0 : row.getAvailableQty(),
                        row.getSafetyStockQty() == null ? 0 : row.getSafetyStockQty()))
                .toList();
    }
}
