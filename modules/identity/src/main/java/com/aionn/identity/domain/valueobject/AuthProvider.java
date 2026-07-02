package com.aionn.identity.domain.valueobject;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;

public enum AuthProvider {
    GOOGLE;

    public static AuthProvider from(String rawProvider) {
        if (rawProvider == null || rawProvider.isBlank()) {
            throw new IdentityException(IdentityErrorCode.PROVIDER_NOT_SUPPORTED);
        }
        try {
            return AuthProvider.valueOf(rawProvider.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IdentityException(IdentityErrorCode.PROVIDER_NOT_SUPPORTED);
        }
    }
}


