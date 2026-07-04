package com.aionn.identity.adapter.rest.dto.security.request;

import jakarta.validation.constraints.NotBlank;

public record MfaSetupRequest(
        @NotBlank(message = "Password is required")
        String password) {
}
