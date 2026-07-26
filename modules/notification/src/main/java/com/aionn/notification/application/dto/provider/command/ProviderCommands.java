package com.aionn.notification.application.dto.provider.command;

import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.application.command.Command;

import java.util.Map;

public final class ProviderCommands {

        private ProviderCommands() {
        }

        public record ConfigureProvider(
                        NotificationChannel channel,
                        String providerType,
                        Map<String, String> config,
                        int rateLimitPerMinute,
                        String configuredBy) implements Command {
        }

        public record UpdateProvider(
                        String providerId,
                        Map<String, String> config,
                        Integer rateLimitPerMinute,
                        Boolean active,
                        String configuredBy) implements Command {
        }
}
