package com.aionn.notification.infrastructure.persistence.adapter.subscription;

import com.aionn.notification.application.port.out.NotificationSubscriptionPersistencePort;
import com.aionn.notification.domain.model.NotificationSubscription;
import com.aionn.notification.infrastructure.persistence.entity.NotificationSubscriptionEntity;
import com.aionn.notification.infrastructure.persistence.mapper.NotificationSubscriptionDomainMapper;
import com.aionn.notification.infrastructure.persistence.repository.NotificationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationSubscriptionPersistenceAdapter implements NotificationSubscriptionPersistencePort {

    private final NotificationSubscriptionRepository jpa;
    private final NotificationSubscriptionDomainMapper mapper;

    @Override
    public NotificationSubscription save(NotificationSubscription subscription) {
        NotificationSubscriptionEntity existing = jpa.findById(subscription.getUserId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(subscription, existing)));
    }

    @Override
    public Optional<NotificationSubscription> findByUserId(String userId) {
        return jpa.findById(userId).map(mapper::toDomain);
    }
}

