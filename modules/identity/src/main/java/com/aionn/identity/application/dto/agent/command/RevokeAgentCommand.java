package com.aionn.identity.application.dto.agent.command;

import com.aionn.sharedkernel.application.command.Command;

public record RevokeAgentCommand(
                String ownerUserId,
                String agentId) implements Command {
}



