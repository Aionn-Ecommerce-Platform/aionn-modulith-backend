package com.aionn.contracts.event.shipping;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record ShipmentRegisteredIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String trackingCode,
        String carrierOrderId,
        Instant occurredAt) implements IntegrationEvent.ShipmentScoped {

    public ShipmentRegisteredIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
