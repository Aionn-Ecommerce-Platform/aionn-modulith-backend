package com.aionn.notification.infrastructure.persistence.repository;

import com.aionn.notification.infrastructure.persistence.entity.NotificationProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationProviderRepository extends JpaRepository<NotificationProviderEntity, String> {
    Optional<NotificationProviderEntity> findFirstByChannelAndActiveTrue(String channel);
}

