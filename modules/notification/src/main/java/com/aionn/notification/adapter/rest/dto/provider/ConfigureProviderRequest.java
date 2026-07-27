package com.aionn.notification.adapter.rest.dto.provider;

import com.aionn.notification.domain.valueobject.NotificationChannel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ConfigureProviderRequest(
        @NotNull NotificationChannel channel,
        @NotBlank String providerType,
        Map<String, String> config,
        @Min(1) int rateLimitPerMinute) {
}

