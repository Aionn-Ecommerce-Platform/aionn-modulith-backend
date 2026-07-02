package com.aionn.identity.adapter.rest.dto.agent.response;

import java.time.LocalDateTime;

public record AgentIdentityResponse(
        String agentId,
        String key,
        String permissions,
        String status,
        LocalDateTime expiryAt,
        LocalDateTime createdAt) {
}


