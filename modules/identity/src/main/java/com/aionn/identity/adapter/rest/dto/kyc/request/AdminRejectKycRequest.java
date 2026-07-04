package com.aionn.identity.adapter.rest.dto.kyc.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRejectKycRequest(
        @NotBlank @Size(max = 500) String reason) {
}
