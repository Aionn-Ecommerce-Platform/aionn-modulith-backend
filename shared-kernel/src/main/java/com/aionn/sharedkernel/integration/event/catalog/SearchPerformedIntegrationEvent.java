package com.aionn.sharedkernel.integration.event.catalog;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;

import java.time.Instant;
import java.util.List;

public record SearchPerformedIntegrationEvent(
        String eventId,
        String userId,
        String keyword,
        List<String> categoryIds,
        String brandId,
        Instant occurredAt) implements IntegrationEvent.UserScoped {

    public SearchPerformedIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        categoryIds = IntegrationEvent.freezeList(categoryIds, "categoryIds");
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
