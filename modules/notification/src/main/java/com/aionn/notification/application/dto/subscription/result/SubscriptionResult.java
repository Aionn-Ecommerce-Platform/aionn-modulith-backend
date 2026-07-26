package com.aionn.notification.application.dto.subscription.result;

import java.time.Instant;
import java.util.Map;

public record SubscriptionResult(
        String userId,
        Map<String, Map<String, Boolean>> settings,
        Instant createdAt,
        Instant updatedAt) {
}

