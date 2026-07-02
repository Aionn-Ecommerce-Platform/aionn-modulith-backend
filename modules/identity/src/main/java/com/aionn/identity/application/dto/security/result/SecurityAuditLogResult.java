package com.aionn.identity.application.dto.security.result;

import java.time.LocalDateTime;

public record SecurityAuditLogResult(
                String auditId,
                String eventType,
                String description,
                String ipAddress,
                String deviceId,
                LocalDateTime timestamp) {
}

