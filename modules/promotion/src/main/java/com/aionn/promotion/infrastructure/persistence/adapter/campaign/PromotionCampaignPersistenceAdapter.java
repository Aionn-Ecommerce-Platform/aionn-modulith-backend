package com.aionn.promotion.infrastructure.persistence.adapter.campaign;

import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.infrastructure.persistence.entity.PromotionCampaignEntity;
import com.aionn.promotion.infrastructure.persistence.mapper.PromotionCampaignDomainMapper;
import com.aionn.promotion.infrastructure.persistence.repository.PromotionCampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PromotionCampaignPersistenceAdapter implements PromotionCampaignPersistencePort {

    private final PromotionCampaignRepository jpa;
    private final PromotionCampaignDomainMapper mapper;

    @Override
    public PromotionCampaign save(PromotionCampaign campaign) {
        PromotionCampaignEntity existing = jpa.findById(campaign.getCampaignId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(campaign, existing)));
    }

    @Override
    public Optional<PromotionCampaign> findById(String campaignId) {
        return jpa.findById(campaignId).map(mapper::toDomain);
    }

    @Override
    public List<PromotionCampaign> findToActivate(Instant now, int limit) {
        return jpa.findToActivate(now, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PromotionCampaign> findToEnd(Instant now, int limit) {
        return jpa.findToEnd(now, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PromotionCampaign> listByStatus(String status, int limit) {
        return jpa.findByStatus(status, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

