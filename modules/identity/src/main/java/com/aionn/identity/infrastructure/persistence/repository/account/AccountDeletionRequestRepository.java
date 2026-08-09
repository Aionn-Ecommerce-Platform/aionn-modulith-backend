package com.aionn.identity.infrastructure.persistence.repository.account;

import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import com.aionn.identity.infrastructure.persistence.entity.AccountDeletionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;

import java.util.Optional;

public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequestEntity, String> {

    Optional<AccountDeletionRequestEntity> findByUser_UserIdAndStatus(String userId, AccountDeletionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AccountDeletionRequestEntity r "
            + "WHERE r.status = :status AND r.scheduledDeletionAt <= :now "
            + "ORDER BY r.scheduledDeletionAt, r.deletionRequestId")
    List<AccountDeletionRequestEntity> findDueForUpdate(
            @Param("status") AccountDeletionStatus status,
            @Param("now") Instant now,
            Pageable pageable);
}


