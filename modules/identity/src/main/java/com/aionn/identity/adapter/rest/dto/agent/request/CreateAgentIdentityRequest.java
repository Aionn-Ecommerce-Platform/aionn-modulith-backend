package com.aionn.identity.adapter.rest.dto.agent.request;

import jakarta.validation.constraints.NotBlank;

public record CreateAgentIdentityRequest(
        @NotBlank(message = "Agent name is required")
        String agentName) {
}


