package com.aionn.promotion.infrastructure.integration;

import com.aionn.promotion.application.port.out.FlashSaleRegistrationPersistencePort;
import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.domain.model.FlashSaleRegistration;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.valueobject.CampaignStatus;
import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.promotion.infrastructure.persistence.repository.PromotionCampaignRepository;
import com.aionn.sharedkernel.integration.port.promotion.FlashSaleQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Promotion-side implementation of the cross-module read port. Catalog reads
 * are wrapped in a short transaction so the JPA fetch joins terminate cleanly.
 */
@Component
@RequiredArgsConstructor
public class FlashSaleQueryAdapter implements FlashSaleQueryPort {

    private final FlashSaleRegistrationPersistencePort registrationRepository;
    private final PromotionCampaignPersistencePort campaignRepository;
    private final PromotionCampaignRepository campaignJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, ProductFlashSale> findActiveByProductIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<FlashSaleRegistration> regs = registrationRepository
                .findApprovedRunningByProductIds(productIds);
        if (regs.isEmpty()) {
            return Map.of();
        }
        // Resolve each registration's campaign to fetch the end-date.
        Map<String, PromotionCampaign> campaigns = new HashMap<>();
        for (FlashSaleRegistration reg : regs) {
            campaigns.computeIfAbsent(reg.getCampaignId(),
                    id -> campaignRepository.findById(id).orElse(null));
        }

        Map<String, List<SkuOffer>> offersByProduct = new HashMap<>();
        Map<String, String> campaignByProduct = new HashMap<>();
        for (FlashSaleRegistration reg : regs) {
            if (!reg.hasStockLeft()) {
                continue;
            }
            offersByProduct.computeIfAbsent(reg.getProductId(), k -> new ArrayList<>())
                    .add(new SkuOffer(
                            reg.getSkuId(),
                            reg.getSalePrice().amount(),
                            reg.getSalePrice().currency(),
                            reg.getSaleStock(),
                            reg.getSoldCount()));
            campaignByProduct.putIfAbsent(reg.getProductId(), reg.getCampaignId());
        }

        Map<String, ProductFlashSale> result = new HashMap<>();
        for (Map.Entry<String, List<SkuOffer>> entry : offersByProduct.entrySet()) {
            String productId = entry.getKey();
            String campaignId = campaignByProduct.get(productId);
            PromotionCampaign campaign = campaigns.get(campaignId);
            if (campaign == null || campaign.getStatus() != CampaignStatus.RUNNING) {
                continue;
            }
            result.put(productId, new ProductFlashSale(
                    productId, campaignId, campaign.getEndDate(), entry.getValue()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveFlashSaleCampaign> listActiveCampaigns(int limit) {
        int safe = Math.max(1, Math.min(limit, 50));
        List<ActiveFlashSaleCampaign> out = new ArrayList<>();
        campaignJpaRepository.findAll().stream()
                .filter(c -> CampaignType.FLASH_SALE.name().equals(c.getType()))
                .filter(c -> CampaignStatus.RUNNING.name().equals(c.getStatus()))
                .limit(safe)
                .forEach(c -> {
                    List<FlashSaleRegistration> regs = registrationRepository
                            .findByCampaign(c.getCampaignId(),
                                    com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus.APPROVED,
                                    200);
                    List<String> productIds = regs.stream()
                            .filter(FlashSaleRegistration::hasStockLeft)
                            .map(FlashSaleRegistration::getProductId)
                            .distinct()
                            .toList();
                    out.add(new ActiveFlashSaleCampaign(
                            c.getCampaignId(),
                            c.getName(),
                            c.getStartDate(),
                            c.getEndDate(),
                            productIds));
                });
        return out;
    }
}
