package com.aionn.sharedkernel.integration.event.shipping;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record ShipmentDispatchedIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String trackingCode,
        Instant occurredAt) implements IntegrationEvent {

    public ShipmentDispatchedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
