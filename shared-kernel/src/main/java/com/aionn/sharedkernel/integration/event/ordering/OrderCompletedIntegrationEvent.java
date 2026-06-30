package com.aionn.sharedkernel.integration.event.ordering;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderCompletedIntegrationEvent(
        String eventId,
        String orderId,
        Instant occurredAt) implements IntegrationEvent {

    public OrderCompletedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
