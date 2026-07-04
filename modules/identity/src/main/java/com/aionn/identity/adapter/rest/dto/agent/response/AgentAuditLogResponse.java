package com.aionn.identity.adapter.rest.dto.agent.response;

import java.time.LocalDateTime;

public record AgentAuditLogResponse(
        String auditId,
        String eventType,
        String description,
        String ipAddress,
        String deviceId,
        LocalDateTime timestamp) {
}


