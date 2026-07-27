package com.aionn.notification.infrastructure.persistence.mapper;

import com.aionn.notification.domain.model.NotificationProvider;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.infrastructure.persistence.entity.NotificationProviderEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationProviderDomainMapper {

    public NotificationProvider toDomain(NotificationProviderEntity e) {
        return new NotificationProvider(
                e.getProviderId(),
                NotificationChannel.valueOf(e.getChannel()),
                e.getProviderType(),
                e.getConfig(),
                e.isActive(),
                e.getRateLimitPerMinute(),
                e.getConfiguredBy(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public NotificationProviderEntity toEntity(NotificationProvider p, NotificationProviderEntity existing) {
        NotificationProviderEntity entity = existing != null ? existing
                : NotificationProviderEntity.builder()
                        .providerId(p.getProviderId())
                        .channel(p.getChannel().name())
                        .providerType(p.getProviderType())
                        .configuredBy(p.getConfiguredBy())
                        .build();
        entity.setConfig(p.getConfig());
        entity.setActive(p.isActive());
        entity.setRateLimitPerMinute(p.getRateLimitPerMinute());
        return entity;
    }
}

