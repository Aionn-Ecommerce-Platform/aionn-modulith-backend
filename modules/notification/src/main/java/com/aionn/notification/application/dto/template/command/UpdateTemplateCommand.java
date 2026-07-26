package com.aionn.notification.application.dto.template.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateTemplateCommand(
        String templateId,
        String subject,
        String content) implements Command {
}
