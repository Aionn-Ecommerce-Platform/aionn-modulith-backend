package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.provider.command.ProviderCommands;
import com.aionn.notification.application.port.out.NotificationProviderPersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.NotificationProvider;
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
public class NotificationProviderService {

    private final NotificationProviderPersistencePort repository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public NotificationProvider configure(ProviderCommands.ConfigureProvider command) {
        NotificationProvider provider = NotificationProvider.configure(IdGenerator.ulid(),
                command.channel(), command.providerType(), command.config(),
                command.rateLimitPerMinute(), command.configuredBy(), clock);
        NotificationProvider saved = repository.save(provider);
        eventPublisher.publish(provider.pullEvents());
        return saved;
    }

    public NotificationProvider update(ProviderCommands.UpdateProvider command) {
        NotificationProvider provider = repository.findById(command.providerId())
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.PROVIDER_NOT_FOUND));
        provider.update(command.config(), command.rateLimitPerMinute(), command.active(),
                command.configuredBy(), clock);
        NotificationProvider saved = repository.save(provider);
        eventPublisher.publish(provider.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationProvider> listAll() {
        return repository.findAll();
    }
}
