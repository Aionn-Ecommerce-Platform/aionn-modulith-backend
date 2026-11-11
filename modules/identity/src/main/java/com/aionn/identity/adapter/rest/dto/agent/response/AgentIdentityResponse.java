package com.aionn.identity.adapter.rest.dto.agent.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentIdentityResponse(
        String agentId,
        String apiKey,
        String permissions,
        String status,
        Instant expiryAt,
        Instant createdAt) {
}
