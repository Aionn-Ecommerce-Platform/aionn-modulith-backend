package com.aionn.identity.application.dto.user.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateDisplayNameCommand(
                String userId,
                String displayName) implements Command {
}

