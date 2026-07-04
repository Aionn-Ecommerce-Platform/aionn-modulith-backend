package com.aionn.identity.adapter.rest.dto.user.request;

import com.aionn.identity.application.policy.IdentityValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeAvatarRequest(
        @NotBlank(message = "Avatar URL is required") @Size(max = IdentityValidationConstants.AVATAR_URL_MAX_LENGTH, message = "Avatar URL exceeds maximum length") String avatarUrl) {
}
