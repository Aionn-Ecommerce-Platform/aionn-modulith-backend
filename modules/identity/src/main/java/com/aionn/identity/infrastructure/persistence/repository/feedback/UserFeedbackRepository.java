package com.aionn.identity.infrastructure.persistence.repository.feedback;

import com.aionn.identity.domain.valueobject.FeedbackCategory;
import com.aionn.identity.domain.valueobject.FeedbackStatus;
import com.aionn.identity.infrastructure.persistence.entity.UserFeedbackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface UserFeedbackRepository extends JpaRepository<UserFeedbackEntity, String> {

    List<UserFeedbackEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<UserFeedbackEntity> findByStatusOrderByCreatedAtDesc(FeedbackStatus status, Pageable pageable);

    Page<UserFeedbackEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT f.status AS status, COUNT(f) AS cnt FROM UserFeedbackEntity f GROUP BY f.status")
    List<FeedbackStatusCount> countByStatus();

    @Query("SELECT f.category AS category, COUNT(f) AS cnt FROM UserFeedbackEntity f GROUP BY f.category")
    List<FeedbackCategoryCount> countByCategory();

    @Query("""
        SELECT f.createdAt AS createdAt, f.handledAt AS handledAt
          FROM UserFeedbackEntity f
         WHERE f.handledAt IS NOT NULL
           AND f.handledAt >= :from
           AND f.handledAt < :to
        """)
    List<FeedbackResolutionProjection> findResolutionsBetween(LocalDateTime from, LocalDateTime to);

    interface FeedbackStatusCount {
        FeedbackStatus getStatus();

        Long getCnt();
    }

    interface FeedbackCategoryCount {
        FeedbackCategory getCategory();

        Long getCnt();
    }

    interface FeedbackResolutionProjection {
        LocalDateTime getCreatedAt();

        LocalDateTime getHandledAt();
    }
}
