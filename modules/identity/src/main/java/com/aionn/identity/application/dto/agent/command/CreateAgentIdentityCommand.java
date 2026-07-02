package com.aionn.identity.application.dto.agent.command;

import com.aionn.sharedkernel.application.command.Command;

public record CreateAgentIdentityCommand(
        String ownerUserId,
        String agentName) implements Command {
}

