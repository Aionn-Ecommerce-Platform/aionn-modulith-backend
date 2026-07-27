package com.aionn.notification.adapter.rest.dto.subscription.response;

import java.time.Instant;
import java.util.Map;

public record SubscriptionResponse(
        String userId,
        Map<String, Map<String, Boolean>> settings,
        Instant createdAt,
        Instant updatedAt) {
}
