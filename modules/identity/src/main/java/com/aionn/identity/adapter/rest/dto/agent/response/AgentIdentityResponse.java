package com.aionn.identity.adapter.rest.dto.agent.response;

import java.time.Instant;

public record AgentIdentityResponse(
        String agentId,
        String keyHash,
        String permissions,
        String status,
        Instant expiryAt,
        Instant createdAt) {
}


