package com.aionn.identity.application.dto.agent.command;

public record UpdateAgentPermissionsCommand(
                String ownerUserId,
                String agentId,
                String permissionsJson) {
}



