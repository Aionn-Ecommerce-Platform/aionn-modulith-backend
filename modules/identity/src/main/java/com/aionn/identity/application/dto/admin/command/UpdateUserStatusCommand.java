package com.aionn.identity.application.dto.admin.command;

import com.aionn.sharedkernel.application.command.Command;
import com.aionn.identity.domain.valueobject.UserStatus;

public record UpdateUserStatusCommand(
                String userId,
                UserStatus status) implements Command {
}


