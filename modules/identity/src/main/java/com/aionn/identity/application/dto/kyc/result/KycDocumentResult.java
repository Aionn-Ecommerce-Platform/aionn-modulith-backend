package com.aionn.identity.application.dto.kyc.result;

import java.time.Instant;

public record KycDocumentResult(
        String documentId,
        String kycId,
        String type,
        String url,
        String publicId,
        String status,
        Instant uploadedAt) {
}
