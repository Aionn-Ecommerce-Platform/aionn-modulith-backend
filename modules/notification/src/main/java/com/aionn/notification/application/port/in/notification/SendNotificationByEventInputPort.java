package com.aionn.notification.application.port.in.notification;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.dto.notification.result.NotificationResult;

import java.util.List;

public interface SendNotificationByEventInputPort {
    List<NotificationResult> execute(NotificationCommands.SendByEvent command);
}