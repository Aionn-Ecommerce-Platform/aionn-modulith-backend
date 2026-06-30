package com.aionn.sharedkernel.integration.event.shipping;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record ShipmentDispatchedIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String trackingCode,
        Instant occurredAt) implements IntegrationEvent {

    public ShipmentDispatchedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
