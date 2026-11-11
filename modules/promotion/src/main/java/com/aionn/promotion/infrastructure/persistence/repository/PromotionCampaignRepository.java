package com.aionn.promotion.infrastructure.persistence.repository;

import com.aionn.promotion.infrastructure.persistence.entity.PromotionCampaignEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaignEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PromotionCampaignEntity c WHERE c.campaignId = :campaignId")
    java.util.Optional<PromotionCampaignEntity> findByIdForUpdate(@Param("campaignId") String campaignId);

    @Query("""
            SELECT c FROM PromotionCampaignEntity c
              WHERE c.status = 'SCHEDULED' AND c.startDate <= :now AND c.endDate > :now
            """)
    List<PromotionCampaignEntity> findToActivate(Instant now, Pageable pageable);

    @Query("""
            SELECT c FROM PromotionCampaignEntity c
              WHERE c.status = 'RUNNING' AND c.endDate <= :now
            """)
    List<PromotionCampaignEntity> findToEnd(Instant now, Pageable pageable);

    List<PromotionCampaignEntity> findByStatus(String status, Pageable pageable);

    List<PromotionCampaignEntity> findByTypeAndStatus(String type, String status, Pageable pageable);

    @Query("""
            SELECT c FROM PromotionCampaignEntity c
              WHERE c.status = 'RUNNING'
                AND c.startDate <= :now
                AND c.endDate > :now
              ORDER BY c.endDate ASC
            """)
    List<PromotionCampaignEntity> findRunning(@Param("now") Instant now, Pageable pageable);
}

