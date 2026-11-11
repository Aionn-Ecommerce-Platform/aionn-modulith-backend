package com.aionn.contracts.event.identity;

import com.aionn.contracts.event.IntegrationEvent;
import java.time.Instant;

public record PasswordResetRequestedIntegrationEvent(
        String eventId, String userId, String resetToken, Instant occurredAt)
        implements IntegrationEvent.UserScoped {

    public PasswordResetRequestedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
