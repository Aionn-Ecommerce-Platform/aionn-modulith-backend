package com.aionn.notification.application.dto.subscription.command;

import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.application.command.Command;

public record UpdateChannelCommand(
        String userId,
        NotificationCategory category,
        NotificationChannel channel,
        boolean enabled) implements Command {
}
