package com.aionn.identity.adapter.rest.dto.kyc.response;

import java.time.LocalDateTime;

public record KycDocumentResponse(
        String documentId,
        String kycId,
        String type,
        String url,
        String publicId,
        String status,
        LocalDateTime uploadedAt) {
}
