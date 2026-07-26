package com.aionn.notification.application.dto.provider.command;

import com.aionn.sharedkernel.application.command.Command;

import java.util.Map;

public record UpdateProviderCommand(
        String providerId,
        Map<String, String> config,
        Integer rateLimitPerMinute,
        Boolean active,
        String configuredBy) implements Command {
}
