package com.aionn.notification.application.dto.template.command;

import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.application.command.Command;

public record CreateTemplateCommand(
        String eventType,
        NotificationChannel channel,
        NotificationCategory category,
        String locale,
        String subject,
        String content) implements Command {
}
