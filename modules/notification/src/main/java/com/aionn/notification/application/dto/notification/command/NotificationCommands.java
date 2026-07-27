package com.aionn.notification.application.dto.notification.command;

import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.application.command.Command;

import java.util.List;
import java.util.Map;

public final class NotificationCommands {

    private NotificationCommands() {
    }

public record SendByEvent(
            String userId,
            String eventType,
            NotificationCategory category,
            List<NotificationChannel> channels,
            String locale,
            String campaignId,
            Map<String, String> context) implements Command {
    }

    public record SendDirectByEvent(
            String userId,
            String eventType,
            NotificationCategory category,
            NotificationChannel channel,
            String recipient,
            String locale,
            String campaignId,
            Map<String, String> context) implements Command {
    }

    public record MarkRead(String userId, String notiId) implements Command {
    }

    public record MarkDeleted(String userId, String notiId) implements Command {
    }

    public record RecordSent(String notiId) implements Command {
    }

    public record RecordFailed(String notiId, String reason) implements Command {
    }
}
