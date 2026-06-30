package com.aionn.sharedkernel.integration.event.catalog;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

public record MerchantActivatedIntegrationEvent(
        String eventId,
        String merchantId,
        String adminId,
        Instant occurredAt) implements IntegrationEvent {

    public MerchantActivatedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
    }
}
