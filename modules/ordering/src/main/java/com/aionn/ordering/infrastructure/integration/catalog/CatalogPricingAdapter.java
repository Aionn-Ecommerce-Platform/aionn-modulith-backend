package com.aionn.ordering.infrastructure.integration.catalog;

import com.aionn.ordering.application.port.out.CatalogPricingGateway;
import com.aionn.sharedkernel.integration.port.catalog.PricingQueryPort;
import com.aionn.sharedkernel.integration.port.inventory.WarehouseSelectorPort;
import com.aionn.sharedkernel.integration.port.promotion.FlashSaleQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CatalogPricingAdapter implements CatalogPricingGateway {

    private final PricingQueryPort pricingQueryPort;
    private final WarehouseSelectorPort warehouseSelector;
    private final FlashSaleQueryPort flashSaleQueryPort;

    @Override
    public Map<String, SkuPricing> resolve(List<String> skuIds) {
        Map<String, PricingQueryPort.SkuPricing> pricing = pricingQueryPort.resolvePricing(skuIds);
        Map<String, FlashSaleQueryPort.SkuFlashSale> flashSales = flashSaleQueryPort.findActiveBySkuIds(skuIds);
        Map<String, SkuPricing> result = new LinkedHashMap<>();
        for (Map.Entry<String, PricingQueryPort.SkuPricing> entry : pricing.entrySet()) {
            PricingQueryPort.SkuPricing p = entry.getValue();
            String warehouseId = warehouseSelector
                    .selectWarehouseForSku(p.merchantId(), p.skuId())
                    .orElse(null);
            FlashSaleQueryPort.SkuFlashSale flashSale = flashSales.get(p.skuId());
            boolean usesFlashSale = flashSale != null && p.currency().equals(flashSale.currency());
            var effectivePrice = usesFlashSale ? flashSale.salePrice() : p.price();
            result.put(entry.getKey(), new SkuPricing(
                    p.skuId(), p.merchantId(), warehouseId, effectivePrice, p.currency(), p.active(), p.categoryIds(),
                    usesFlashSale ? flashSale.registrationId() : null));
        }
        return result;
    }
}
