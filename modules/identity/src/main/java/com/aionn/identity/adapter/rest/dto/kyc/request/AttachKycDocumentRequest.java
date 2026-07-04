package com.aionn.identity.adapter.rest.dto.kyc.request;

import jakarta.validation.constraints.NotBlank;

public record AttachKycDocumentRequest(
        @NotBlank(message = "Document type is required")
        String documentType,

        @NotBlank(message = "Document URL is required")
        String url,

        String publicId) {
}
