package com.aionn.sharedkernel.integration.event.ordering;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record OrderApprovedIntegrationEvent(
        String eventId,
        String orderId,
        String paymentId,
        Instant occurredAt) implements IntegrationEvent {

    public OrderApprovedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
