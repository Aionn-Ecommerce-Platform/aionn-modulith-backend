package com.aionn.notification.adapter.rest.dto.provider.response;

import java.time.Instant;
import java.util.Map;

public record ProviderResponse(
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
