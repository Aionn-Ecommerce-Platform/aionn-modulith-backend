package com.aionn.identity.application.dto.agent.result;

import java.time.Instant;

public record AgentAuditLogResult(
                String auditId,
                String eventType,
                String description,
                String ipAddress,
                String deviceId,
                Instant timestamp) {
}



