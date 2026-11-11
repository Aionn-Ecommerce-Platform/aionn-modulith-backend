package com.aionn.contracts.event.catalog;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;

public record MerchantClosedIntegrationEvent(
        String eventId,
        String merchantId,
        String reason,
        Instant occurredAt) implements IntegrationEvent.MerchantScoped {

    public MerchantClosedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
