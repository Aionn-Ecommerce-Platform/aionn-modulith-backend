package com.aionn.ordering.infrastructure.integration.catalog;

import com.aionn.ordering.application.port.out.CatalogPricingGateway;
import com.aionn.sharedkernel.integration.port.catalog.PricingQueryPort;
import com.aionn.sharedkernel.integration.port.inventory.WarehouseSelectorPort;
import com.aionn.sharedkernel.integration.port.promotion.FlashSaleQueryPort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogPricingAdapterTest {

    @Mock
    private PricingQueryPort pricingQueryPort;

    @Mock
    private WarehouseSelectorPort warehouseSelector;

    @Mock
    private FlashSaleQueryPort flashSaleQueryPort;

    @InjectMocks
    private CatalogPricingAdapter adapter;

    @Test
    void resolveReturnsMappedSkuPricing() {
        PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                "sku-1", "m-1", BigDecimal.valueOf(100), "VND", true);
        when(pricingQueryPort.resolvePricing(List.of("sku-1")))
                .thenReturn(Map.of("sku-1", pricing));
        when(flashSaleQueryPort.findActiveBySkuIds(List.of("sku-1"))).thenReturn(Map.of());
        when(warehouseSelector.selectWarehouseForSku("m-1", "sku-1"))
                .thenReturn(Optional.of("wh-1"));

        Map<String, CatalogPricingGateway.SkuPricing> result = adapter.resolve(List.of("sku-1"));

        assertEquals(1, result.size());
        CatalogPricingGateway.SkuPricing mapped = result.get("sku-1");
        assertEquals("sku-1", mapped.skuId());
        assertEquals("m-1", mapped.merchantId());
        assertEquals("wh-1", mapped.warehouseId());
        assertEquals(BigDecimal.valueOf(100), mapped.price());
        assertEquals("VND", mapped.currency());
        assertTrue(mapped.active());
    }

    @Test
    void resolveUsesActiveFlashSalePrice() {
        PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                "sku-1", "m-1", BigDecimal.valueOf(100), "VND", true);
        when(pricingQueryPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1", pricing));
        when(flashSaleQueryPort.findActiveBySkuIds(List.of("sku-1"))).thenReturn(Map.of(
                "sku-1", new FlashSaleQueryPort.SkuFlashSale(
                        "sku-1", "registration-1", "campaign-1", BigDecimal.valueOf(75), "VND",
                        java.time.Instant.parse("2026-09-03T00:00:00Z"), 10)));
        when(warehouseSelector.selectWarehouseForSku("m-1", "sku-1")).thenReturn(Optional.of("wh-1"));

        CatalogPricingGateway.SkuPricing result = adapter.resolve(List.of("sku-1")).get("sku-1");

        assertEquals(BigDecimal.valueOf(75), result.price());
        assertEquals("registration-1", result.flashSaleRegistrationId());
    }
}
