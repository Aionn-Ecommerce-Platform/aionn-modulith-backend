package com.aionn.notification.application.port.out;

import com.aionn.notification.domain.valueobject.NotificationChannel;

import java.util.List;

public interface RecipientResolver {

    String resolve(String userId, NotificationChannel channel);

    /** Multiple destinations for fan-out channels (one per active device for PUSH). */
    default List<String> resolveAll(String userId, NotificationChannel channel) {
        return List.of(resolve(userId, channel));
    }
}

