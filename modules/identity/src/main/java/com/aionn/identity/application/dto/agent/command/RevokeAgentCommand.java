package com.aionn.identity.application.dto.agent.command;

public record RevokeAgentCommand(
                String ownerUserId,
                String agentId) {
}



