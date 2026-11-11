package com.aionn.identity.infrastructure.persistence.repository.account;

import com.aionn.identity.domain.valueobject.DataExportStatus;
import com.aionn.identity.infrastructure.persistence.entity.DataExportRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;

public interface DataExportRequestRepository extends JpaRepository<DataExportRequestEntity, String> {

    boolean existsByUser_UserIdAndStatusIn(String userId, Collection<DataExportStatus> statuses);

    /**
     * A request that was never picked up (for example, a previous broken deployment) must not lock
     * the user out of the feature forever. Stale active rows are failed before a new export starts.
     */
    @Modifying
    @Query("UPDATE DataExportRequestEntity e SET e.status = :failedStatus, e.completedAt = :now "
            + "WHERE e.user.userId = :userId AND e.status IN :activeStatuses "
            + "AND e.requestedAt < :cutoff")
    int failStaleRequests(@Param("userId") String userId,
            @Param("failedStatus") DataExportStatus failedStatus,
            @Param("activeStatuses") Collection<DataExportStatus> activeStatuses,
            @Param("cutoff") Instant cutoff,
            @Param("now") Instant now);
}
