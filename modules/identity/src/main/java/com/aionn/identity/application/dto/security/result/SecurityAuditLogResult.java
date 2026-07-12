package com.aionn.identity.application.dto.security.result;

import java.time.Instant;

public record SecurityAuditLogResult(
                String auditId,
                String eventType,
                String description,
                String ipAddress,
                String deviceId,
                Instant timestamp) {
}

