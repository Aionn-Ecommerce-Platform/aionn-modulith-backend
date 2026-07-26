package com.aionn.promotion.infrastructure.persistence.repository;

import com.aionn.promotion.infrastructure.persistence.entity.PromotionCampaignEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaignEntity, String> {

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
}

