package com.aionn.notification.application.dto.subscription.command;

import com.aionn.sharedkernel.application.command.Command;

public record RegisterDeviceTokenCommand(
        String userId,
        String deviceToken,
        String os) implements Command {
}
