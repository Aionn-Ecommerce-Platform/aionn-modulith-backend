package com.aionn.recommendation.infrastructure.integration.inventory;

import com.aionn.recommendation.application.port.out.RecommendationStockAvailabilityPort;
import com.aionn.contracts.port.inventory.StockAvailabilityQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

@Slf4j
@Component("recommendationStockAvailabilityAdapter")
@RequiredArgsConstructor
public class InventoryStockAvailabilityAdapter implements RecommendationStockAvailabilityPort {

    private final StockAvailabilityQueryPort inventoryStockAvailability;

    @Override
    public Set<String> filterAvailableSkus(Collection<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Set.of();
        }
        try {
            return inventoryStockAvailability.filterAvailable(skuIds);
        } catch (RuntimeException exception) {
            log.warn("Availability check failed for {} sku(s); serving unfiltered: {}",
                    skuIds.size(), exception.getMessage());
            return Set.copyOf(skuIds);
        }
    }
}
