package com.aionn.identity.adapter.rest.dto.agent.response;

import java.time.Instant;

public record AgentIdentityResponse(
        String agentId,
        // Hashed credential, not a usable key — kept in the response only so
        // clients can display a stable fingerprint. Name reflects the content.
        String keyHash,
        String permissions,
        String status,
        Instant expiryAt,
        Instant createdAt) {
}


