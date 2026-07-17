package com.aionn.inventory.application.usecase.analytics;

import com.aionn.inventory.application.dto.analytics.result.LowStockAlertResult;
import com.aionn.inventory.application.port.in.inventory.GetMerchantLowStockInputPort;
import com.aionn.inventory.application.service.InventoryAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetMerchantLowStockUseCase implements GetMerchantLowStockInputPort {

    private final InventoryAnalyticsService inventoryAnalyticsService;

    @Override
    @Transactional(readOnly = true)
    public List<LowStockAlertResult> execute(String ownerId) {
        return inventoryAnalyticsService.getMerchantLowStock(ownerId);
    }
}
