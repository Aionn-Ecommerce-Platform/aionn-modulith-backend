package com.aionn.sharedkernel.integration.event.identity;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import java.time.Instant;

public record PasswordResetRequestedIntegrationEvent(
        String eventId, String userId, String resetToken, Instant occurredAt)
        implements IntegrationEvent.UserScoped {

    public PasswordResetRequestedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
