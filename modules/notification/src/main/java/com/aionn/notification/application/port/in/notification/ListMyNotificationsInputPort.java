package com.aionn.notification.application.port.in.notification;

import com.aionn.notification.application.dto.notification.result.NotificationResult;

import java.util.List;

public interface ListMyNotificationsInputPort {
    List<NotificationResult> execute(String userId, int limit);
}