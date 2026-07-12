package com.aionn.identity.adapter.rest.dto.consent.response;

import java.time.Instant;

public record ConsentResponse(
        String consentId,
        String consentType,
        String version,
        Instant agreedAt,
        Instant revokedAt,
        String ipAddress) {
}


