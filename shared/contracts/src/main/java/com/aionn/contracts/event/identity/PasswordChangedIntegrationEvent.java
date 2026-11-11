package com.aionn.contracts.event.identity;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record PasswordChangedIntegrationEvent(
        String eventId,
        String userId,
        String channelHint,
        Instant occurredAt) implements IntegrationEvent.UserScoped {

    public PasswordChangedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
