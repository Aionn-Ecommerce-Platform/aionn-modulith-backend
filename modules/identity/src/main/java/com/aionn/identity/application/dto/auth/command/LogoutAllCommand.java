package com.aionn.identity.application.dto.auth.command;

import com.aionn.sharedkernel.application.command.Command;

public record LogoutAllCommand(
                String userId) implements Command {
}



