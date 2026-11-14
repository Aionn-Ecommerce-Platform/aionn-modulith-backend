package com.aionn.contracts.event.shipping;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record ShipmentReturnedIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String userId,
        String merchantId,
        String reason,
        Instant occurredAt) implements IntegrationEvent.ShipmentScoped {

    public ShipmentReturnedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }

    public ShipmentReturnedIntegrationEvent(
            String eventId,
            String shipmentId,
            String orderId,
            String reason,
            Instant occurredAt) {
        this(eventId, shipmentId, orderId, null, null, reason, occurredAt);
    }
}
