package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.template.command.TemplateCommands;
import com.aionn.notification.application.dto.template.result.TemplateResult;
import com.aionn.notification.application.mapper.NotificationResultMapper;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.notification.application.port.out.NotificationTemplatePersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationTemplateService {

    private final NotificationTemplatePersistencePort repository;
    private final NotificationResultMapper mapper;
    private final EventPublisher eventPublisher;

    public TemplateResult create(TemplateCommands.CreateTemplate command) {
        String locale = command.locale() == null ? "vi-VN" : command.locale();
        if (repository.findByEventChannelLocale(command.eventType(), command.channel(), locale).isPresent()) {
            throw new NotificationException(NotificationErrorCode.TEMPLATE_DUPLICATE);
        }
        NotificationTemplate t = NotificationTemplate.create(IdGenerator.ulid(),
                command.eventType(), command.channel(), command.category(), locale,
                command.subject(), command.content());
        NotificationTemplate saved = repository.save(t);
        eventPublisher.publish(t.pullEvents());
        return mapper.toResult(saved);
    }

    public TemplateResult update(TemplateCommands.UpdateTemplate command) {
        NotificationTemplate t = repository.findById(command.templateId())
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.TEMPLATE_NOT_FOUND));
        t.update(command.subject(), command.content());
        NotificationTemplate saved = repository.save(t);
        eventPublisher.publish(t.pullEvents());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public TemplateResult get(String templateId) {
        return mapper.toResult(repository.findById(templateId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.TEMPLATE_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public java.util.List<TemplateResult> list(int limit) {
        return repository.findAll(limit).stream().map(mapper::toResult).toList();
    }
}
