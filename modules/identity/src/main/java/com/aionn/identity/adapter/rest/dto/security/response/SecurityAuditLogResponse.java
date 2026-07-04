package com.aionn.identity.adapter.rest.dto.security.response;

import java.time.LocalDateTime;

public record SecurityAuditLogResponse(
        String auditId,
        String eventType,
        String description,
        String ipAddress,
        String deviceId,
        LocalDateTime timestamp
) {
}


