package com.aionn.notification.application.dto.subscription.command;

import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.application.command.Command;

public final class SubscriptionCommands {

        private SubscriptionCommands() {
        }

        public record UpdateChannel(
                        String userId,
                        NotificationCategory category,
                        NotificationChannel channel,
                        boolean enabled) implements Command {
        }

        public record RegisterDeviceToken(
                        String userId,
                        String deviceToken,
                        String os) implements Command {
        }

        public record RemoveDeviceToken(String userId, String tokenId) implements Command {
        }
}
