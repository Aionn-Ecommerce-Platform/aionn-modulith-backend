package com.aionn.identity.domain.valueobject;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;

public enum FeedbackCategory {
    GENERAL,
    BUG,
    FEATURE_REQUEST,
    COMPLAINT,
    CONTACT;

    public static FeedbackCategory from(String raw) {
        if (raw == null || raw.isBlank()) {
            return GENERAL;
        }
        try {
            return FeedbackCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IdentityException(IdentityErrorCode.INVALID_USER_STATUS,
                    "Unsupported feedback category: " + raw);
        }
    }
}
