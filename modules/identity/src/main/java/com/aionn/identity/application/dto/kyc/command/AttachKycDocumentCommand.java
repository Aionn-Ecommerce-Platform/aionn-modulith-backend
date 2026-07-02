package com.aionn.identity.application.dto.kyc.command;

public record AttachKycDocumentCommand(
        String userId,
        String kycId,
        String documentType,
        String url,
        String publicId) {
}
