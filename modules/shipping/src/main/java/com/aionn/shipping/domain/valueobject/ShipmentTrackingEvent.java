package com.aionn.shipping.domain.valueobject;

import java.time.Instant;

public record ShipmentTrackingEvent(
        String eventId,
        String shipmentId,
        String status,
        String description,
        String location,
        Instant occurredAt) {
}
