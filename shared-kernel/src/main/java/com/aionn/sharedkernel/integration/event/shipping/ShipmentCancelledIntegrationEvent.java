package com.aionn.sharedkernel.integration.event.shipping;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record ShipmentCancelledIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String reason,
        Instant occurredAt) implements IntegrationEvent.ShipmentScoped {

    public ShipmentCancelledIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
