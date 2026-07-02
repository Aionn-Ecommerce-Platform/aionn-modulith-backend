package com.aionn.identity.adapter.rest.dto.user.request;

import jakarta.validation.constraints.NotBlank;

public record RequestEmailChangeOtpRequest(
        @NotBlank(message = "New email is required")
        String newEmail) {
}
