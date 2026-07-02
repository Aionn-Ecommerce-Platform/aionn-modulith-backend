package com.aionn.identity.adapter.rest.dto.user.request;

import jakarta.validation.constraints.NotBlank;

public record RequestPhoneChangeOtpRequest(
        @NotBlank(message = "New phone is required")
        String newPhone) {
}
