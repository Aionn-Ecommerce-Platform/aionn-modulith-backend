package com.aionn.inventory.infrastructure.integration.inventory;

import com.aionn.inventory.application.port.out.SafetyStockNotifier;
import com.aionn.sharedkernel.integration.event.inventory.SafetyStockBreachedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class InventorySafetyStockNotifier implements SafetyStockNotifier {

    private final IntegrationEventPublisher integrationEventPublisher;
    private final Clock clock;

    @Override
    public void notifySafetyStockBreach(
            String merchantId, String skuId, String warehouseId, int availableQty, int safetyStockQty) {
        integrationEventPublisher.publish(new SafetyStockBreachedIntegrationEvent(
                IdGenerator.ulid(), clock.instant(), merchantId, skuId, warehouseId, availableQty, safetyStockQty));
    }
}
