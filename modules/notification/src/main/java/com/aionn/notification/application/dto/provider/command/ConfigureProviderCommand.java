package com.aionn.notification.application.dto.provider.command;

import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.application.command.Command;

import java.util.Map;

public record ConfigureProviderCommand(
        NotificationChannel channel,
        String providerType,
        Map<String, String> config,
        int rateLimitPerMinute,
        String configuredBy) implements Command {
}
