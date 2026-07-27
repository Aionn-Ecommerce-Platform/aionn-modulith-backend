package com.aionn.notification.application.port.in.notification;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.dto.notification.result.NotificationResult;

public interface DeleteNotificationInputPort {
    NotificationResult execute(NotificationCommands.MarkDeleted command);
}