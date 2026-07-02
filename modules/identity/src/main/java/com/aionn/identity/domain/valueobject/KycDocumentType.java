package com.aionn.identity.domain.valueobject;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;

public enum KycDocumentType {
    ID_FRONT,
    ID_BACK,
    SELFIE,
    PASSPORT,
    OTHER;

    public static KycDocumentType from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IdentityException(IdentityErrorCode.KYC_DOCUMENT_REQUIRED, "KYC document type is required");
        }
        try {
            return KycDocumentType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IdentityException(IdentityErrorCode.KYC_DOCUMENT_REQUIRED,
                    "Unsupported KYC document type: " + raw);
        }
    }
}
