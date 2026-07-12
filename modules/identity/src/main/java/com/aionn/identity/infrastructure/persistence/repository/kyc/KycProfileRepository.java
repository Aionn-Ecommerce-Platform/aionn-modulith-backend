package com.aionn.identity.infrastructure.persistence.repository.kyc;

import com.aionn.identity.infrastructure.persistence.entity.KycProfileEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface KycProfileRepository extends JpaRepository<KycProfileEntity, String> {

    List<KycProfileEntity> findByUser_UserIdOrderBySubmittedAtDesc(String userId);

    Optional<KycProfileEntity> findByKycIdAndUser_UserId(String kycId, String userId);

    Optional<KycProfileEntity> findByProviderApplicantId(String providerApplicantId);

    List<KycProfileEntity> findByStatusOrderBySubmittedAtDesc(String status, Pageable pageable);

    @Query("SELECT k.status AS status, COUNT(k) AS cnt FROM KycProfileEntity k GROUP BY k.status")
    List<KycStatusCount> countByStatus();

    @Query("""
        SELECT k.submittedAt AS submittedAt, k.approvedAt AS approvedAt
          FROM KycProfileEntity k
         WHERE k.submittedAt IS NOT NULL
           AND k.approvedAt IS NOT NULL
           AND k.approvedAt >= :from
           AND k.approvedAt < :to
        """)
    List<KycDecisionProjection> findDecisionsBetween(Instant from, Instant to);

    interface KycStatusCount {
        String getStatus();

        Long getCnt();
    }

    interface KycDecisionProjection {
        Instant getSubmittedAt();

        Instant getApprovedAt();
    }
}
