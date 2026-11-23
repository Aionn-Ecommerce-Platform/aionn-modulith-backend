package com.aionn.notification.infrastructure.persistence.repository;

import com.aionn.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    List<NotificationEntity> findByUserIdAndStatusNotOrderByCreatedAtDesc(
            String userId, String excludedStatus, Pageable pageable);

    List<NotificationEntity> findByUserIdAndChannelAndStatusNotOrderByCreatedAtDesc(
            String userId, String channel, String excludedStatus, Pageable pageable);

    long countByUserIdAndChannelAndStatusNot(String userId, String channel, String excludedStatus);

    @Query("""
        SELECT COUNT(n) FROM NotificationEntity n
         WHERE n.userId = :userId
           AND n.channel = 'IN_APP'
           AND n.status NOT IN ('DELETED', 'READ')
        """)
    long countUnreadInApp(String userId);

    @Query("""
            SELECT n FROM NotificationEntity n
              WHERE n.status = 'PENDING' AND n.retryCount < :maxRetry
            ORDER BY n.createdAt ASC
            """)
    List<NotificationEntity> findRetryable(@Param("maxRetry") int maxRetry, Pageable pageable);

    long countByCampaignIdAndStatus(String campaignId, String status);
}

