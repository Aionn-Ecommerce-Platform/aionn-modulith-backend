package com.aionn.notification.infrastructure.persistence.mapper;

import com.aionn.notification.domain.model.NotificationSubscription;
import com.aionn.notification.infrastructure.persistence.entity.NotificationSubscriptionEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationSubscriptionDomainMapper {

    public NotificationSubscription toDomain(NotificationSubscriptionEntity e) {
        return new NotificationSubscription(e.getUserId(), e.getSettings(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public NotificationSubscriptionEntity toEntity(NotificationSubscription s,
            NotificationSubscriptionEntity existing) {
        NotificationSubscriptionEntity entity = existing != null ? existing
                : NotificationSubscriptionEntity.builder()
                        .userId(s.getUserId())
                        .build();
        entity.setSettings(s.rawSettings());
        return entity;
    }
}

