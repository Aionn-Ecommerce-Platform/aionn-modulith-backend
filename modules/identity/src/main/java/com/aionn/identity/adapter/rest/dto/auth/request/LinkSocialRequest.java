package com.aionn.identity.adapter.rest.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LinkSocialRequest(
        @NotBlank(message = "Provider is required")
        @Pattern(regexp = "^(?i)google$", message = "Provider must be google")
        String provider,
        @NotBlank(message = "Provider token is required")
        String providerToken) {
}

