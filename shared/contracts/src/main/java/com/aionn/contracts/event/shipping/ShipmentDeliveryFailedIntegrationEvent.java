package com.aionn.contracts.event.shipping;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record ShipmentDeliveryFailedIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String userId,
        String reason,
        int attemptCount,
        Instant occurredAt) implements IntegrationEvent.ShipmentScoped {

    public ShipmentDeliveryFailedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }

    public ShipmentDeliveryFailedIntegrationEvent(
            String eventId,
            String shipmentId,
            String orderId,
            String reason,
            int attemptCount,
            Instant occurredAt) {
        this(eventId, shipmentId, orderId, null, reason, attemptCount, occurredAt);
    }
}
