package com.aionn.sharedkernel.integration.event.ordering;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderApprovedIntegrationEvent(
        String eventId,
        String orderId,
        String paymentId,
        Instant occurredAt) implements IntegrationEvent {

    public OrderApprovedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
