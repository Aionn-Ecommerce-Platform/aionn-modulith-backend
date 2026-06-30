package com.aionn.sharedkernel.integration.event.shipping;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

/**
 * Published when a shipment has been registered with the carrier (tracking
 * code assigned).
 */
public record ShipmentRegisteredIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String trackingCode,
        String carrierOrderId,
        Instant occurredAt) implements IntegrationEvent {

    public ShipmentRegisteredIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
