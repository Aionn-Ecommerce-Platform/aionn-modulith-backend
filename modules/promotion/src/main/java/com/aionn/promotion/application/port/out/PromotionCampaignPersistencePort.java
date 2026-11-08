package com.aionn.promotion.application.port.out;

import com.aionn.promotion.domain.model.PromotionCampaign;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PromotionCampaignPersistencePort {

    PromotionCampaign save(PromotionCampaign campaign);

    Optional<PromotionCampaign> findById(String campaignId);

    List<PromotionCampaign> findToActivate(Instant now, int limit);

    List<PromotionCampaign> findToEnd(Instant now, int limit);

    List<PromotionCampaign> listByStatus(String status, int limit);

    /**
     * Returns campaigns that are RUNNING and inside their start/end window,
     * ordered by the nearest end date. Used by storefront listings so the
     * campaign limit is applied in the database instead of after filtering.
     */
    List<PromotionCampaign> findRunning(Instant now, int limit);
}
