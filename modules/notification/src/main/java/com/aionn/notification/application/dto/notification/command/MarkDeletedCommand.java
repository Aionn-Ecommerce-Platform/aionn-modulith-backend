package com.aionn.notification.application.dto.notification.command;

import com.aionn.sharedkernel.application.command.Command;

public record MarkDeletedCommand(String userId, String notiId) implements Command {
}
