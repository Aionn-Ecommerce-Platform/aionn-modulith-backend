package com.aionn.notification.application.dto.template.command;

import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.application.command.Command;

public final class TemplateCommands {

        private TemplateCommands() {
        }

        public record CreateTemplate(
                        String eventType,
                        NotificationChannel channel,
                        NotificationCategory category,
                        String locale,
                        String subject,
                        String content) implements Command {
        }

        public record UpdateTemplate(
                        String templateId,
                        String subject,
                        String content) implements Command {
        }
}
