package com.aionn.notification.application.port.in.notification;

public interface RetryPendingNotificationsInputPort {
    int execute(int batchSize);
}