package com.aionn.identity.adapter.rest.dto.security.request;

import jakarta.validation.constraints.NotBlank;

public record MfaToggleRequest(
        @NotBlank(message = "Password is required")
        String password,
        @NotBlank(message = "MFA code is required")
        String mfaCode) {
}

