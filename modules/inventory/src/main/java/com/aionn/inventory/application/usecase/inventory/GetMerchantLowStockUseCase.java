package com.aionn.inventory.application.usecase.inventory;

import com.aionn.inventory.application.dto.analytics.result.LowStockAlertResult;
import com.aionn.inventory.application.port.in.inventory.GetMerchantLowStockInputPort;
import com.aionn.inventory.application.service.InventoryAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetMerchantLowStockUseCase implements GetMerchantLowStockInputPort {

    private final InventoryAnalyticsService analyticsService;

    @Override
    public List<LowStockAlertResult> execute(String ownerId) {
        return analyticsService.getMerchantLowStock(ownerId);
    }
}
