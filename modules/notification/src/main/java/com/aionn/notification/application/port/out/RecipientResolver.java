package com.aionn.notification.application.port.out;

import com.aionn.notification.domain.valueobject.NotificationChannel;

public interface RecipientResolver {

    String resolve(String userId, NotificationChannel channel);
}

