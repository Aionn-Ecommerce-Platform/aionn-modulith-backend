package com.aionn.notification.application.port.out;

import java.time.Instant;

public interface NotificationRealtimePort {

    void publishCreated(String userId, String notificationId, Instant createdAt);
}
