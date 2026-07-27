package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.template.command.TemplateCommands;
import com.aionn.notification.application.port.out.NotificationTemplatePersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationTemplateService {

    private final NotificationTemplatePersistencePort repository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public NotificationTemplate create(TemplateCommands.CreateTemplate command) {
        String locale = command.locale() == null ? "vi-VN" : command.locale();
        if (repository.findByEventChannelLocale(command.eventType(), command.channel(), locale).isPresent()) {
            throw new NotificationException(NotificationErrorCode.TEMPLATE_DUPLICATE);
        }
        NotificationTemplate template = NotificationTemplate.create(IdGenerator.ulid(),
                command.eventType(), command.channel(), command.category(), locale,
                command.subject(), command.content(), clock);
        NotificationTemplate saved = repository.save(template);
        eventPublisher.publish(template.pullEvents());
        return saved;
    }

    public NotificationTemplate update(TemplateCommands.UpdateTemplate command) {
        NotificationTemplate template = required(command.templateId());
        template.update(command.subject(), command.content(), clock);
        NotificationTemplate saved = repository.save(template);
        eventPublisher.publish(template.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public NotificationTemplate get(String templateId) {
        return required(templateId);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplate> list(int limit) {
        return repository.findAll(limit);
    }

    private NotificationTemplate required(String templateId) {
        return repository.findById(templateId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.TEMPLATE_NOT_FOUND));
    }
}
