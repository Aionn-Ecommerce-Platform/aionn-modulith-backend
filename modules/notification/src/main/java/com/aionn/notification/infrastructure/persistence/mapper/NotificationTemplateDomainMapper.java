package com.aionn.notification.infrastructure.persistence.mapper;

import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateDomainMapper {

    public NotificationTemplate toDomain(NotificationTemplateEntity e) {
        return new NotificationTemplate(
                e.getTemplateId(),
                e.getEventType(),
                NotificationChannel.valueOf(e.getChannel()),
                NotificationCategory.valueOf(e.getCategory()),
                e.getLocale(),
                e.getSubject(),
                e.getContent(),
                e.getPlaceholders(),
                e.getVersion(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public NotificationTemplateEntity toEntity(NotificationTemplate t, NotificationTemplateEntity existing) {
        NotificationTemplateEntity entity = existing != null ? existing
                : NotificationTemplateEntity.builder()
                        .templateId(t.getTemplateId())
                        .eventType(t.getEventType())
                        .channel(t.getChannel().name())
                        .category(t.getCategory().name())
                        .locale(t.getLocale())
                        .build();
        entity.setSubject(t.getSubject());
        entity.setContent(t.getContent());
        entity.setPlaceholders(t.getPlaceholders());
        entity.setVersion(t.getVersion());
        entity.setActive(t.isActive());
        return entity;
    }
}

