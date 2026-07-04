package com.aionn.identity.adapter.rest.dto.agent.request;

import com.aionn.identity.adapter.rest.validation.ValidJson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAgentPermissionsRequest(
        @NotBlank(message = "Permissions JSON is required")
        @Size(max = 4096, message = "Permissions JSON must be at most 4096 characters")
        @ValidJson(message = "Permissions must be well-formed JSON")
        String permissionsJson) {
}
