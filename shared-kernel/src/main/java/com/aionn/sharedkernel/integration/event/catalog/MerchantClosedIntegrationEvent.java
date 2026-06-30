package com.aionn.sharedkernel.integration.event.catalog;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record MerchantClosedIntegrationEvent(
        String eventId,
        String merchantId,
        String reason,
        Instant occurredAt) implements IntegrationEvent {

    public MerchantClosedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
