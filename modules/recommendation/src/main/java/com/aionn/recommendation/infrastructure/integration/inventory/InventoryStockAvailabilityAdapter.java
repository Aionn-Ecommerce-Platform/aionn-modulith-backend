package com.aionn.recommendation.infrastructure.integration.inventory;

import com.aionn.recommendation.application.port.out.StockAvailabilityQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * Bridges the module's availability port to the shared inventory port.
 *
 * <p>Fails open: if inventory cannot answer, the slate is served unfiltered rather than emptied. An
 * occasional out-of-stock recommendation is a much smaller failure than an empty home feed, and this
 * is a display decision, not a checkout one.
 */
@Slf4j
@Component("recommendationStockAvailabilityAdapter")
@RequiredArgsConstructor
public class InventoryStockAvailabilityAdapter implements StockAvailabilityQueryPort {

    private final com.aionn.sharedkernel.integration.port.inventory.StockAvailabilityQueryPort
            inventoryStockAvailability;

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
