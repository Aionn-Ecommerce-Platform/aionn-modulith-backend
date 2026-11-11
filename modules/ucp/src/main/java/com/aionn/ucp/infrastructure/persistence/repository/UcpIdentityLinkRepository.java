package com.aionn.ucp.infrastructure.persistence.repository;

import com.aionn.ucp.infrastructure.persistence.entity.UcpIdentityLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UcpIdentityLinkRepository extends JpaRepository<UcpIdentityLinkEntity, String> {

    Optional<UcpIdentityLinkEntity> findByPlatformIdAndPlatformSubject(String platformId, String platformSubject);

    Optional<UcpIdentityLinkEntity> findFirstByCustomerId(String customerId);

    /**
     * Marks the link REVOKED only when a matching active link exists. Atomic — a
     * concurrent save can't resurrect it between check and write.
     *
     * @return rows updated (0 = no such link)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UcpIdentityLinkEntity e
               SET e.status = 'REVOKED', e.expiresAt = :revokedAt
             WHERE e.platformId = :platformId
               AND e.platformSubject = :platformSubject
               AND e.status = 'ACTIVE'
            """)
    int revokeByPlatformAndSubject(@Param("platformId") String platformId,
            @Param("platformSubject") String platformSubject,
            @Param("revokedAt") Instant revokedAt);
}
