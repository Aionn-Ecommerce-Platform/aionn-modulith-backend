package com.aionn.notification.infrastructure.persistence.adapter.template;

import com.aionn.notification.application.port.out.NotificationTemplatePersistencePort;
import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.aionn.notification.infrastructure.persistence.mapper.NotificationTemplateDomainMapper;
import com.aionn.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationTemplatePersistenceAdapter implements NotificationTemplatePersistencePort {

    private final NotificationTemplateRepository jpa;
    private final NotificationTemplateDomainMapper mapper;

    @Override
    public NotificationTemplate save(NotificationTemplate template) {
        NotificationTemplateEntity existing = jpa.findById(template.getTemplateId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(template, existing)));
    }

    @Override
    public Optional<NotificationTemplate> findById(String templateId) {
        return jpa.findById(templateId).map(mapper::toDomain);
    }

    @Override
    public Optional<NotificationTemplate> findByEventChannelLocale(
            String eventType, NotificationChannel channel, String locale) {
        return jpa.findByEventTypeAndChannelAndLocale(eventType, channel.name(), locale)
                .map(mapper::toDomain);
    }

    @Override
    public List<NotificationTemplate> findAll(int limit) {
        return jpa.findAll(PageRequest.of(0, Math.max(1, limit),
                Sort.by(Sort.Direction.DESC, "updatedAt"))).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

