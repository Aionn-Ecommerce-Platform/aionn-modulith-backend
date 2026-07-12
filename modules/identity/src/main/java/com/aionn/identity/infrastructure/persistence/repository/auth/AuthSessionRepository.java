package com.aionn.identity.infrastructure.persistence.repository.auth;

import com.aionn.identity.infrastructure.persistence.entity.AuthSessionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, String> {

    List<AuthSessionEntity> findByUser_UserIdOrderByCreatedAtDesc(String userId);

    List<AuthSessionEntity> findByUser_UserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    default List<AuthSessionEntity> findRecentByUserId(String userId, Pageable pageable) {
        return findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Modifying
    @Query("DELETE FROM AuthSessionEntity s WHERE COALESCE(s.lastActiveAt, s.createdAt) < :cutoff")
    int deleteIdleBefore(@Param("cutoff") Instant cutoff);
}
