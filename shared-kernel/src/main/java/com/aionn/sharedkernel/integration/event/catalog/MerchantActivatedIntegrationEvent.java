package com.aionn.sharedkernel.integration.event.catalog;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;

public record MerchantActivatedIntegrationEvent(
        String eventId,
        String merchantId,
        String adminId,
        Instant occurredAt) implements IntegrationEvent {

    public MerchantActivatedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
