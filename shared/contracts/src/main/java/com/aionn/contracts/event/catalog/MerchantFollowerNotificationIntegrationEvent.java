package com.aionn.contracts.event.catalog;

import com.aionn.contracts.event.IntegrationEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Requests a follower notification for every user following a merchant. The notification module
 * resolves the follower list through {@code MerchantFollowerQueryPort}.
 */
public record MerchantFollowerNotificationIntegrationEvent(
        String eventId,
        String merchantId,
        String notificationType,
        String title,
        String message,
        String targetUrl,
        Instant occurredAt) implements IntegrationEvent.MerchantScoped {

    public MerchantFollowerNotificationIntegrationEvent {
        eventId = IntegrationEvent.requireEventId(eventId);
        merchantId = Objects.requireNonNull(merchantId, "merchantId must not be null");
        notificationType = Objects.requireNonNull(notificationType, "notificationType must not be null");
        title = Objects.requireNonNull(title, "title must not be null");
        message = Objects.requireNonNull(message, "message must not be null");
        occurredAt = IntegrationEvent.defaultOccurredAt(occurredAt);
    }
}
