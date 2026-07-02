package com.aionn.identity.application.dto.consent.result;

import java.time.LocalDateTime;

public record ConsentResult(
        String consentId,
        String userId,
        String consentType,
        String version,
        boolean agreed,
        LocalDateTime agreedAt,
        LocalDateTime revokedAt,
        String ipAddress) {
}

