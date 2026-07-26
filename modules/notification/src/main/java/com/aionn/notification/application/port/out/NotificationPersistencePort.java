package com.aionn.notification.application.port.out;

import com.aionn.notification.domain.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationPersistencePort {

    Notification save(Notification notification);

    Optional<Notification> findById(String notiId);

    List<Notification> findByUser(String userId, int limit);

List<Notification> findRetryable(int limit);

long countByCampaignAndStatus(String campaignId, String status);
}

