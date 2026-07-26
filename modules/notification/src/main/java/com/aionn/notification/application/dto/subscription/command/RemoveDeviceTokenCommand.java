package com.aionn.notification.application.dto.subscription.command;

import com.aionn.sharedkernel.application.command.Command;

public record RemoveDeviceTokenCommand(String userId, String tokenId) implements Command {
}
