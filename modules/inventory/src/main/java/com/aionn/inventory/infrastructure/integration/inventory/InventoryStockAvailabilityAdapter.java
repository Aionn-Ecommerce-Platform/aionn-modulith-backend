package com.aionn.inventory.infrastructure.integration.inventory;

import com.aionn.inventory.application.port.out.InventoryItemPersistencePort;
import com.aionn.inventory.domain.model.InventoryItem;
import com.aionn.sharedkernel.integration.port.inventory.StockAvailabilityQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Read-only availability lookup for listing and filtering paths. Deliberately holds no lock and
 * creates no reservation: callers use it to decide what to display, and a display decision must not
 * take stock away from a real checkout.
 */
@Component
@RequiredArgsConstructor
public class InventoryStockAvailabilityAdapter implements StockAvailabilityQueryPort {

    private final InventoryItemPersistencePort itemRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<String> filterAvailable(Collection<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Set.of();
        }
        Set<String> available = new LinkedHashSet<>();
        for (String skuId : new LinkedHashSet<>(skuIds)) {
            // A SKU counts as available when any warehouse can ship it; which warehouse serves the
            // order is decided later by the warehouse selector.
            for (InventoryItem item : itemRepository.findBySku(skuId)) {
                if (item.getAvailableQty() > 0 && !item.isLocked()) {
                    available.add(skuId);
                    break;
                }
            }
        }
        return available;
    }
}
