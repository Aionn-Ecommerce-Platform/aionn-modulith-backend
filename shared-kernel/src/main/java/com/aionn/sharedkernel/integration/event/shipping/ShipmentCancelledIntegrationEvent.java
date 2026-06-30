package com.aionn.sharedkernel.integration.event.shipping;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

/**
 * Published when a shipment has been cancelled.
 */
public record ShipmentCancelledIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public ShipmentCancelledIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
