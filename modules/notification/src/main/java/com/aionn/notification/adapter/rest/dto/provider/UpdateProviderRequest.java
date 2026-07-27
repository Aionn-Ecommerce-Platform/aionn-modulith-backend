package com.aionn.notification.adapter.rest.dto.provider;

import jakarta.validation.constraints.Min;

import java.util.Map;

public record UpdateProviderRequest(
        Map<String, String> config,
        @Min(1) Integer rateLimitPerMinute,
        Boolean active) {
}

