package com.aionn.chat.infrastructure.persistence.repository;

import com.aionn.chat.infrastructure.persistence.entity.UserBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlockEntity, String> {

    Optional<UserBlockEntity> findByBlockerIdAndBlockedIdAndActiveTrue(String blockerId, String blockedId);

    boolean existsByBlockerIdAndBlockedIdAndActiveTrue(String blockerId, String blockedId);

    List<UserBlockEntity> findByBlockerIdOrderByCreatedAtDesc(String blockerId);
}

