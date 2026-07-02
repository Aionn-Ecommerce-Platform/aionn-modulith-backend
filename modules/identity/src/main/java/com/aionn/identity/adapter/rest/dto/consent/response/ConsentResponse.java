package com.aionn.identity.adapter.rest.dto.consent.response;

import java.time.LocalDateTime;

public record ConsentResponse(
        String consentId,
        String consentType,
        String version,
        LocalDateTime agreedAt,
        LocalDateTime revokedAt,
        String ipAddress) {
}


