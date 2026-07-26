package com.aionn.notification.infrastructure.persistence.adapter.notification;

import com.aionn.notification.application.port.out.NotificationPersistencePort;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.infrastructure.persistence.entity.NotificationEntity;
import com.aionn.notification.infrastructure.persistence.mapper.NotificationDomainMapper;
import com.aionn.notification.infrastructure.persistence.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationPersistencePort {

    private final NotificationRepository jpa;
    private final NotificationDomainMapper mapper;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity existing = jpa.findById(notification.getNotiId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(notification, existing)));
    }

    @Override
    public Optional<Notification> findById(String notiId) {
        return jpa.findById(notiId).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findByUser(String userId, int limit) {
        return jpa.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, "DELETED",
                PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findRetryable(int limit) {
        return jpa.findRetryable(PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByCampaignAndStatus(String campaignId, String status) {
        return jpa.countByCampaignIdAndStatus(campaignId, status);
    }
}

