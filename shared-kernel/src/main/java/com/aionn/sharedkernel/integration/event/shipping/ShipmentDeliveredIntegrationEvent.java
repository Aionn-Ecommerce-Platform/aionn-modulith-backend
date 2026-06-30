package com.aionn.sharedkernel.integration.event.shipping;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record ShipmentDeliveredIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String signatureUrl,
        Instant deliveredAt,
        Instant occurredAt) implements IntegrationEvent {

    public ShipmentDeliveredIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
