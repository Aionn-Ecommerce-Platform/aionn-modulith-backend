package com.aionn.identity.adapter.rest.dto.agent.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateAgentPermissionsRequest(
        @NotBlank(message = "Permissions JSON is required")
        String permissionsJson) {
}


