package com.aionn.identity.adapter.rest.dto.user.request;

import com.aionn.identity.application.policy.IdentityValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeDisplayNameRequest(
        @NotBlank(message = "Display name is required") @Size(min = IdentityValidationConstants.DISPLAY_NAME_MIN_LENGTH, max = IdentityValidationConstants.DISPLAY_NAME_MAX_LENGTH, message = "Display name must be 2-100 characters") String displayName) {
}
