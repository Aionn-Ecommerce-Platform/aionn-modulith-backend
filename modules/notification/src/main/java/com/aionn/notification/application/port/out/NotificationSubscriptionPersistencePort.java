package com.aionn.notification.application.port.out;

import com.aionn.notification.domain.model.NotificationSubscription;

import java.util.Optional;

public interface NotificationSubscriptionPersistencePort {

    NotificationSubscription save(NotificationSubscription subscription);

    Optional<NotificationSubscription> findByUserId(String userId);
}

