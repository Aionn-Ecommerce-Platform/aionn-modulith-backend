package com.aionn.notification.application.port.out;

import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.notification.domain.valueobject.NotificationChannel;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplatePersistencePort {

    NotificationTemplate save(NotificationTemplate template);

    Optional<NotificationTemplate> findById(String templateId);

    Optional<NotificationTemplate> findByEventChannelLocale(String eventType, NotificationChannel channel,
            String locale);

    List<NotificationTemplate> findAll(int limit);
}

