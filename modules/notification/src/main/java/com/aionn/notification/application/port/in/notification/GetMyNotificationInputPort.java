package com.aionn.notification.application.port.in.notification;

import com.aionn.notification.application.dto.notification.result.NotificationResult;

public interface GetMyNotificationInputPort {
    NotificationResult execute(String userId, String notiId);
}