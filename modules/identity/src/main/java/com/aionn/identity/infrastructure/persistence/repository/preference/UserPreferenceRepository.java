package com.aionn.identity.infrastructure.persistence.repository.preference;

import com.aionn.identity.infrastructure.persistence.entity.UserPreferenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM UserPreferenceEntity p WHERE p.userId = :userId")
    Optional<UserPreferenceEntity> findByIdForUpdate(@Param("userId") String userId);
}



