package com.aionn.identity.domain.valueobject;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;

public enum KycReviewAnswer {
    GREEN,
    RED;

    public static KycReviewAnswer from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return KycReviewAnswer.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_ERROR,
                    "Unsupported KYC review answer: " + raw);
        }
    }
}
