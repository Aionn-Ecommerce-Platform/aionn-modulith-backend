package com.aionn.sharedkernel.integration.event.shipping;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record ShipmentDeliveredIntegrationEvent(
        String eventId,
        String shipmentId,
        String orderId,
        String userId,
        String signatureUrl,
        Instant deliveredAt,
        Instant expectedDeliveryDate,
        boolean lateDelivery,
        Instant occurredAt) implements IntegrationEvent.ShipmentScoped {

    public ShipmentDeliveredIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }

    public ShipmentDeliveredIntegrationEvent(
            String eventId,
            String shipmentId,
            String orderId,
            String signatureUrl,
            Instant deliveredAt,
            Instant occurredAt) {
        this(eventId, shipmentId, orderId, null, signatureUrl, deliveredAt, null, false, occurredAt);
    }
}
