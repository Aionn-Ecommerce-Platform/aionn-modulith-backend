package com.aionn.notification.infrastructure.persistence.mapper;

import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.domain.valueobject.NotificationPriority;
import com.aionn.notification.domain.valueobject.NotificationStatus;
import com.aionn.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationDomainMapper {

    public Notification toDomain(NotificationEntity e) {
        return new Notification(
                e.getNotiId(), e.getUserId(), e.getTemplateId(),
                NotificationChannel.valueOf(e.getChannel()),
                NotificationCategory.valueOf(e.getCategory()),
                NotificationPriority.valueOf(e.getPriority()),
                e.getSubject(), e.getContent(), e.getCampaignId(),
                NotificationStatus.valueOf(e.getStatus()),
                e.getRetryCount(), e.getLastFailureReason(),
                e.getCreatedAt(), e.getUpdatedAt(),
                e.getSentAt(), e.getReadAt(), e.getDeletedAt());
    }

    public NotificationEntity toEntity(Notification n, NotificationEntity existing) {
        NotificationEntity entity = existing != null ? existing
                : NotificationEntity.builder()
                        .notiId(n.getNotiId())
                        .userId(n.getUserId())
                        .templateId(n.getTemplateId())
                        .channel(n.getChannel().name())
                        .category(n.getCategory().name())
                        .priority(n.getPriority().name())
                        .subject(n.getSubject())
                        .content(n.getContent())
                        .campaignId(n.getCampaignId())
                        .build();
        entity.setStatus(n.getStatus().name());
        entity.setRetryCount(n.getRetryCount());
        entity.setLastFailureReason(n.getLastFailureReason());
        entity.setSentAt(n.getSentAt());
        entity.setReadAt(n.getReadAt());
        entity.setDeletedAt(n.getDeletedAt());
        return entity;
    }
}

