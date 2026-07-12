package com.aionn.identity.application.dto.agent.result;

import java.time.Instant;

public record AgentIdentityResult(
                String agentId,
                String keyHash,
                String permissions,
                String status,
                Instant expiryAt,
                Instant createdAt) {
}

