package com.aionn.identity.adapter.rest.dto.kyc.request;

import jakarta.validation.constraints.NotBlank;

public record CreateKycRequest(
        @NotBlank(message = "Document type is required")
        String docType) {
}


