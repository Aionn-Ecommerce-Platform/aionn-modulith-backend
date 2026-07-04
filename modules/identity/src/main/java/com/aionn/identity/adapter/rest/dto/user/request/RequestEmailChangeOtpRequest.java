package com.aionn.identity.adapter.rest.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestEmailChangeOtpRequest(
        @NotBlank(message = "New email is required")
        @Email(message = "New email must be a valid email address")
        String newEmail) {
}
