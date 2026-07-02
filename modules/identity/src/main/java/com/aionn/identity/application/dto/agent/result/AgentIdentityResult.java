package com.aionn.identity.application.dto.agent.result;

import java.time.LocalDateTime;

public record AgentIdentityResult(
                String agentId,
                String keyHash,
                String permissions,
                String status,
                LocalDateTime expiryAt,
                LocalDateTime createdAt) {
}

