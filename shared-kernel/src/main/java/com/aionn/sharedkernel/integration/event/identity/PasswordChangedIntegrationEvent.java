package com.aionn.sharedkernel.integration.event.identity;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record PasswordChangedIntegrationEvent(
        String eventId,
        String userId,
        String channelHint,
        Instant occurredAt) implements IntegrationEvent {

    public PasswordChangedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
