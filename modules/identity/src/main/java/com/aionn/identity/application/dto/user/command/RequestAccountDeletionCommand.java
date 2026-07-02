package com.aionn.identity.application.dto.user.command;

import com.aionn.sharedkernel.application.command.Command;

public record RequestAccountDeletionCommand(String userId) implements Command {
}



