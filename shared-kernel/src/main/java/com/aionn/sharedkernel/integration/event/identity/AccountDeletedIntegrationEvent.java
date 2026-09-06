package com.aionn.sharedkernel.integration.event.identity;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

/**
 * Emitted once an account deletion request has completed its grace period and identity has
 * tombstoned the account. Consumers holding behavioural or derived personal data keyed by
 * {@code userId} must erase it; consumers holding historical business records keep them, because the
 * opaque user ID is deliberately retained for those.
 */
public record AccountDeletedIntegrationEvent(
        String eventId,
        String userId,
        Instant occurredAt) implements IntegrationEvent.UserScoped {

    public AccountDeletedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
