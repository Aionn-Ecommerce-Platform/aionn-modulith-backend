package com.aionn.notification.application.dto.notification.command;

import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.application.command.Command;

import java.util.List;
import java.util.Map;

public record SendByEventCommand(
        String userId,
        String eventType,
        NotificationCategory category,
        List<NotificationChannel> channels,
        String locale,
        String campaignId,
        Map<String, String> context) implements Command {
}
