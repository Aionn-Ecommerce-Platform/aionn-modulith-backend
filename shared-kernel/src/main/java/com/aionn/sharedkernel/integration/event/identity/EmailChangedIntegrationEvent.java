package com.aionn.sharedkernel.integration.event.identity;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record EmailChangedIntegrationEvent(
        String eventId,
        String userId,
        String oldEmail,
        String newEmail,
        Instant occurredAt) implements IntegrationEvent {

    public EmailChangedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
