package com.aionn.inventory.infrastructure.integration;

import com.aionn.inventory.application.port.out.OutboundOrderNotifier;
import com.aionn.sharedkernel.integration.event.inventory.StockCommittedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReservationFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class InventoryOutboundOrderNotifier implements OutboundOrderNotifier {

    private final IntegrationEventPublisher integrationEventPublisher;
    private final Clock clock;

    @Override
    public void notifyOutbound(String orderId, String reservationId, String skuId, String warehouseId, int qty) {
        integrationEventPublisher.publish(new StockCommittedIntegrationEvent(
                com.aionn.sharedkernel.util.IdGenerator.ulid(), reservationId, skuId, warehouseId, orderId, qty, clock.instant()));
    }

    @Override
    public void notifyReservationFailed(String orderId, String skuId, String warehouseId, int qty, String reason) {
        integrationEventPublisher.publish(new StockReservationFailedIntegrationEvent(
                com.aionn.sharedkernel.util.IdGenerator.ulid(), skuId, warehouseId, orderId, qty, reason, clock.instant()));
    }
}
