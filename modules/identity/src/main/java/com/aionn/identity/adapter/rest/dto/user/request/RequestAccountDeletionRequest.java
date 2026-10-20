package com.aionn.identity.adapter.rest.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record RequestAccountDeletionRequest(
        @Schema(description = "Account password for verification", example = "Password123!")
        String password,

        @Schema(description = "6-digit MFA code (required if MFA is enabled)", example = "123456")
        String mfaCode) {
}
