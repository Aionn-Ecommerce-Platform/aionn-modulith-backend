package com.aionn.notification.application.dto.provider.result;

import java.time.Instant;
import java.util.Map;

public record ProviderResult(
        String providerId,
        String channel,
        String providerType,
        Map<String, String> config,
        boolean active,
        int rateLimitPerMinute,
        String configuredBy,
        Instant createdAt,
        Instant updatedAt) {
}

