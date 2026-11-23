package com.aionn.notification.application.port.in.notification;

import com.aionn.notification.application.dto.notification.result.NotificationPageResult;
import com.aionn.notification.application.dto.notification.result.NotificationResult;

import java.util.List;

public interface ListMyNotificationsInputPort {
    List<NotificationResult> execute(String userId, int limit);

    NotificationPageResult executePaged(String userId, int page, int limit);
}
