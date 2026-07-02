package com.aionn.identity.adapter.rest.dto.security.request;

import jakarta.validation.constraints.NotBlank;

public record UnlockAccountRequest(
        @NotBlank(message = "User ID is required")
        String userId) {
}


