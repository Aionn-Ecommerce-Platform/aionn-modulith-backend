package com.aionn.shipping.application.dto.shipment.result;

import java.time.Instant;

public record ShipmentEventResult(
        String eventId,
        String shipmentId,
        String status,
        String description,
        String location,
        Instant occurredAt) {
}
