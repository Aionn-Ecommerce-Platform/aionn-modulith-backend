package com.aionn.sharedkernel.integration.event.ordering;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderShippedIntegrationEvent(
        String eventId,
        String orderId,
        String shipmentId,
        Instant occurredAt) implements IntegrationEvent {

    public OrderShippedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
