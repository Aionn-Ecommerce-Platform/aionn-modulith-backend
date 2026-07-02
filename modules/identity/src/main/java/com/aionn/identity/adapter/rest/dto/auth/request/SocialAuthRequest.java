package com.aionn.identity.adapter.rest.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

public record SocialAuthRequest(
        @NotBlank(message = "Provider is required")
        String provider,
        @NotBlank(message = "Provider token is required")
        String providerToken) {
}


