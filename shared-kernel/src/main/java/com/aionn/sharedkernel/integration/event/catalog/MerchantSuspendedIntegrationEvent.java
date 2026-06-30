package com.aionn.sharedkernel.integration.event.catalog;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record MerchantSuspendedIntegrationEvent(
        String eventId,
        String merchantId,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public MerchantSuspendedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
