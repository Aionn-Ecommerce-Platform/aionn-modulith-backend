package com.aionn.promotion.application.port.out;

import com.aionn.promotion.domain.model.PromotionCampaign;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PromotionCampaignPersistencePort {

    PromotionCampaign save(PromotionCampaign campaign);

    Optional<PromotionCampaign> findById(String campaignId);

    /** Row-locked read used when consuming campaign budget concurrently. */
    Optional<PromotionCampaign> findByIdForUpdate(String campaignId);

    List<PromotionCampaign> findToActivate(Instant now, int limit);

    List<PromotionCampaign> findToEnd(Instant now, int limit);

    List<PromotionCampaign> listByStatus(String status, int limit);

    List<PromotionCampaign> findRunning(Instant now, int limit);

    List<PromotionCampaign> findStorefrontSlots(Instant now, int limit);
}
