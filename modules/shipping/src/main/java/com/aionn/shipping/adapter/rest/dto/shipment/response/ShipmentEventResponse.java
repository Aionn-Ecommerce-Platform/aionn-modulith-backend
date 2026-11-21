package com.aionn.shipping.adapter.rest.dto.shipment.response;

import java.time.Instant;

public record ShipmentEventResponse(
        String eventId,
        String shipmentId,
        String status,
        String description,
        String location,
        Instant occurredAt) {
}
