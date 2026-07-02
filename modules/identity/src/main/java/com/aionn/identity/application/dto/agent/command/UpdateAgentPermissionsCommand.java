package com.aionn.identity.application.dto.agent.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateAgentPermissionsCommand(
                String ownerUserId,
                String agentId,
                String permissionsJson) implements Command {
}



