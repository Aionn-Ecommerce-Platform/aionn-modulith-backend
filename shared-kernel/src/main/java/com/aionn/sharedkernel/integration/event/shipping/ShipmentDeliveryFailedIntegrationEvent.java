package com.aionn.sharedkernel.integration.event.shipping;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record ShipmentDeliveryFailedIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String reason,
        int attemptCount,
        Instant occurredAt) implements IntegrationEvent {

    public ShipmentDeliveryFailedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
