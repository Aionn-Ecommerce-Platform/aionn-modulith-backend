package com.aionn.identity.application.dto.consent.result;

import java.time.Instant;

public record ConsentResult(
        String consentId,
        String userId,
        String consentType,
        String version,
        boolean agreed,
        Instant agreedAt,
        Instant revokedAt,
        String ipAddress) {
}

