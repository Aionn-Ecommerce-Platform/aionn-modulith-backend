package com.aionn.identity.adapter.rest.dto.security.response;

import java.time.Instant;

public record SecurityAuditLogResponse(
        String auditId,
        String eventType,
        String description,
        String ipAddress,
        String deviceId,
        Instant timestamp
) {
}


