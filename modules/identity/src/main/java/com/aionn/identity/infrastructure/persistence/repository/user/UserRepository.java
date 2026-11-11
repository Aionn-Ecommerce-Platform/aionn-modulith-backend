package com.aionn.identity.infrastructure.persistence.repository.user;

import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {

        Optional<UserEntity> findByEmailIgnoreCase(String email);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT u FROM UserEntity u WHERE u.userId = :userId")
        Optional<UserEntity> findByIdForUpdate(@Param("userId") String userId);

        Optional<UserEntity> findByPhone(String phone);

        Optional<UserEntity> findByUsernameIgnoreCase(String username);

        boolean existsByPhone(String phone);

        boolean existsByUsernameIgnoreCase(String username);

        @Query("SELECT u FROM UserEntity u " +
                        "WHERE (:status IS NULL OR u.status = :status) " +
                        "AND (:role IS NULL OR :role MEMBER OF u.roles) " +
                        "ORDER BY u.createdAt DESC")
        Page<UserEntity> findUsersWithFilters(@Param("status") UserStatus status,
                        @Param("role") UserRole role,
                        Pageable pageable);

        @Query("SELECT u.createdAt FROM UserEntity u WHERE u.createdAt >= :from AND u.createdAt < :to")
        List<Instant> findCreatedAtBetween(@Param("from") Instant from,
                        @Param("to") Instant to);

        @Query("SELECT u.status AS status, COUNT(u) AS cnt FROM UserEntity u GROUP BY u.status")
        List<StatusCount> countByStatus();

        /**
         * Atomically increments the failed-login counter and applies the lock once the
         * threshold is reached. A read-modify-write from the caller would let concurrent
         * guesses all write the same value and bypass the lockout.
         */
        @Modifying
        @Query("UPDATE UserEntity u SET "
                        + "u.failedLoginAttempts = u.failedLoginAttempts + 1, "
                        + "u.lockedUntil = CASE WHEN u.failedLoginAttempts + 1 >= :maxAttempts "
                        + "THEN :lockedUntil ELSE u.lockedUntil END "
                        + "WHERE u.userId = :userId")
        int incrementFailedLoginAttempts(@Param("userId") String userId,
                        @Param("maxAttempts") int maxAttempts,
                        @Param("lockedUntil") Instant lockedUntil);

        @Query("SELECT r AS role, COUNT(u) AS cnt FROM UserEntity u JOIN u.roles r GROUP BY r")
        List<RoleCount> countByRole();

        interface StatusCount {
                UserStatus getStatus();

                Long getCnt();
        }

        interface RoleCount {
                UserRole getRole();

                Long getCnt();
        }
}
