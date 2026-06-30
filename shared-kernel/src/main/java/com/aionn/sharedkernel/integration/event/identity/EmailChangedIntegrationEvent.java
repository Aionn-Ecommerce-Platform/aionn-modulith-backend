package com.aionn.sharedkernel.integration.event.identity;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record EmailChangedIntegrationEvent(
        String eventId,
        String userId,
        String oldEmail,
        String newEmail,
        Instant occurredAt) implements IntegrationEvent {

    public EmailChangedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
