package com.aionn.notification.application.port.out;

import com.aionn.notification.domain.valueobject.NotificationChannel;

public interface DeliveryAttemptPort {

    enum Status { STARTED, SUCCEEDED }

    record Attempt(String attemptId, Status status, boolean created) {}

    Attempt begin(String notificationId, NotificationChannel channel);

    void recordSucceeded(String attemptId, String providerMessageId);

    void recordFailed(String attemptId, String error);
}
