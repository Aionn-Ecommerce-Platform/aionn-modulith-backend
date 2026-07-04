package com.aionn.identity.adapter.rest.dto.user.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmOtpRequest(
        @NotBlank(message = "OTP code is required")
        String otpCode) {
}
