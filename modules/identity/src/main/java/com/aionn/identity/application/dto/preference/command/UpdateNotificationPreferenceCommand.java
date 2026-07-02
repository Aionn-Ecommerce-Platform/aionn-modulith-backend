package com.aionn.identity.application.dto.preference.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateNotificationPreferenceCommand(
                String userId,
                String notificationSettingsJson) implements Command {
}

