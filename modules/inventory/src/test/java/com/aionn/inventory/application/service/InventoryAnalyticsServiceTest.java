package com.aionn.inventory.application.service;

import com.aionn.inventory.application.dto.analytics.result.LowStockAlertResult;
import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.application.port.out.InventoryItemPersistencePort.LowStockItem;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAnalyticsServiceTest {

    @Mock
    private InventoryItemPersistencePort inventoryRepository;

    @Mock
    private MerchantQueryPort merchantQueryPort;

    @InjectMocks
    private InventoryAnalyticsService analyticsService;

    @Test
    void getMerchantLowStockReturnsResultList() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("merchant-1"));

        LowStockItem row = new LowStockItem("sku-1", "wh-1", 5, 3, 10);

        when(inventoryRepository.findLowStockForMerchant("merchant-1")).thenReturn(List.of(row));

        List<LowStockAlertResult> results = analyticsService.getMerchantLowStock("owner-1");

        assertEquals(1, results.size());
        LowStockAlertResult result = results.get(0);
        assertEquals("sku-1", result.skuId());
        assertEquals("wh-1", result.warehouseId());
        assertEquals(5, result.physicalQty());
        assertEquals(3, result.availableQty());
        assertEquals(10, result.safetyStockQty());
    }

    @Test
    void getMerchantLowStockThrowsExceptionWhenMerchantNotFound() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> analyticsService.getMerchantLowStock("owner-1"));
        verifyNoInteractions(inventoryRepository);
    }
}
