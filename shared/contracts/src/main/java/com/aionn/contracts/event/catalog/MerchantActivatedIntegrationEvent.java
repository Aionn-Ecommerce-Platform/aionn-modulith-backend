package com.aionn.contracts.event.catalog;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record MerchantActivatedIntegrationEvent(
        String eventId,
        String merchantId,
        String adminId,
        Instant occurredAt) implements IntegrationEvent.MerchantScoped {

    public MerchantActivatedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
