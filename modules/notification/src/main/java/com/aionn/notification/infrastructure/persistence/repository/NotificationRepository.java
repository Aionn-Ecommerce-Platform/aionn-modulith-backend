package com.aionn.notification.infrastructure.persistence.repository;

import com.aionn.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    List<NotificationEntity> findByUserIdAndStatusNotOrderByCreatedAtDesc(
            String userId, String excludedStatus, Pageable pageable);

    @Query("""
            SELECT n FROM NotificationEntity n
              WHERE n.status = 'PENDING' AND n.retryCount < 3
            ORDER BY n.createdAt ASC
            """)
    List<NotificationEntity> findRetryable(Pageable pageable);

    long countByCampaignIdAndStatus(String campaignId, String status);
}

