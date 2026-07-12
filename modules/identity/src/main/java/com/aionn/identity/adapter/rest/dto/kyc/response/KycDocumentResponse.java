package com.aionn.identity.adapter.rest.dto.kyc.response;

import java.time.Instant;

public record KycDocumentResponse(
        String documentId,
        String kycId,
        String type,
        String url,
        String publicId,
        String status,
        Instant uploadedAt) {
}
