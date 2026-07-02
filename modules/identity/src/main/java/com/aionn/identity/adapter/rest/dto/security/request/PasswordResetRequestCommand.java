package com.aionn.identity.adapter.rest.dto.security.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestCommand(
        @NotBlank(message = "Identity is required")
        String identity) {
}


