package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.provider.command.ProviderCommands;
import com.aionn.notification.application.dto.provider.result.ProviderResult;
import com.aionn.notification.application.mapper.NotificationResultMapper;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.notification.application.port.out.NotificationProviderPersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.NotificationProvider;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationProviderService {

    private final NotificationProviderPersistencePort repository;
    private final NotificationResultMapper mapper;
    private final EventPublisher eventPublisher;

    public ProviderResult configure(ProviderCommands.ConfigureProvider command) {
        NotificationProvider provider = NotificationProvider.configure(IdGenerator.ulid(),
                command.channel(), command.providerType(), command.config(),
                command.rateLimitPerMinute(), command.configuredBy());
        NotificationProvider saved = repository.save(provider);
        eventPublisher.publish(provider.pullEvents());
        return mapper.toResult(saved);
    }

    public ProviderResult update(ProviderCommands.UpdateProvider command) {
        NotificationProvider provider = repository.findById(command.providerId())
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.PROVIDER_NOT_FOUND));
        provider.update(command.config(), command.rateLimitPerMinute(), command.active(), command.configuredBy());
        NotificationProvider saved = repository.save(provider);
        eventPublisher.publish(provider.pullEvents());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderResult> listAll() {
        return repository.findAll().stream().map(mapper::toResult).toList();
    }
}

