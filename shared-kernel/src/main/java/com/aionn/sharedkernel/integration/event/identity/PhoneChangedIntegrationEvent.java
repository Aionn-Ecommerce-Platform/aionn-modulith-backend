package com.aionn.sharedkernel.integration.event.identity;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record PhoneChangedIntegrationEvent(
        String eventId,
        String userId,
        String oldPhone,
        String newPhone,
        Instant occurredAt) implements IntegrationEvent.UserScoped {

    public PhoneChangedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
