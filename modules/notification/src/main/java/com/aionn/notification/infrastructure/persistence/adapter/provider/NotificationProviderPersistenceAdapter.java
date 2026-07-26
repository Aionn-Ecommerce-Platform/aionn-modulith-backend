package com.aionn.notification.infrastructure.persistence.adapter.provider;

import com.aionn.notification.application.port.out.NotificationProviderPersistencePort;
import com.aionn.notification.domain.model.NotificationProvider;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.infrastructure.persistence.entity.NotificationProviderEntity;
import com.aionn.notification.infrastructure.persistence.mapper.NotificationProviderDomainMapper;
import com.aionn.notification.infrastructure.persistence.repository.NotificationProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationProviderPersistenceAdapter implements NotificationProviderPersistencePort {

    private final NotificationProviderRepository jpa;
    private final NotificationProviderDomainMapper mapper;

    @Override
    public NotificationProvider save(NotificationProvider provider) {
        NotificationProviderEntity existing = jpa.findById(provider.getProviderId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(provider, existing)));
    }

    @Override
    public Optional<NotificationProvider> findById(String providerId) {
        return jpa.findById(providerId).map(mapper::toDomain);
    }

    @Override
    public Optional<NotificationProvider> findActiveByChannel(NotificationChannel channel) {
        return jpa.findFirstByChannelAndActiveTrue(channel.name()).map(mapper::toDomain);
    }

    @Override
    public List<NotificationProvider> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }
}

