package com.aionn.identity.application.dto.agent.result;

import java.time.LocalDateTime;

public record AgentAuditLogResult(
                String auditId,
                String eventType,
                String description,
                String ipAddress,
                String deviceId,
                LocalDateTime timestamp) {
}



