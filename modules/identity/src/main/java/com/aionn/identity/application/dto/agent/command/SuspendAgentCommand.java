package com.aionn.identity.application.dto.agent.command;

public record SuspendAgentCommand(
                String ownerUserId,
                String agentId) {
}



