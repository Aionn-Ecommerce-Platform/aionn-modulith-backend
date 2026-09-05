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
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

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
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void reserve(String orderId, List<Allocation> allocations) {
        for (Allocation allocation : allocations) {
            Integer existingQuantity = jdbcTemplate.query(
                    "SELECT quantity FROM flash_sale_allocations WHERE order_id = ? AND registration_id = ?",
                    rs -> rs.next() ? rs.getInt(1) : null,
                    orderId, allocation.registrationId());
            if (existingQuantity != null) {
                if (existingQuantity != allocation.quantity()) {
                    throw new IllegalStateException("Flash-sale allocation does not match the original order request");
                }
                continue;
            }
            int updated = jdbcTemplate.update("""
                    UPDATE flash_sale_registrations r
                       SET sold_count = sold_count + ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                     WHERE r.registration_id = ?
                       AND r.status = 'APPROVED'
                       AND r.sold_count + ? <= r.sale_stock
                       AND EXISTS (
                           SELECT 1 FROM promotion_campaigns c
                            WHERE c.campaign_id = r.campaign_id
                              AND c.status = 'RUNNING'
                              AND c.start_date <= CURRENT_TIMESTAMP
                              AND c.end_date > CURRENT_TIMESTAMP)
                    """, allocation.quantity(), allocation.registrationId(), allocation.quantity());
            if (updated != 1) {
                throw new CapacityExceededException(allocation.registrationId());
            }
            jdbcTemplate.update("""
                    INSERT INTO flash_sale_allocations(order_id, registration_id, quantity, created_at)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                    """, orderId, allocation.registrationId(), allocation.quantity());
        }
    }

    @Override
    @Transactional
    public void release(String orderId) {
        List<Allocation> allocations = jdbcTemplate.query("""
                SELECT registration_id, quantity
                  FROM flash_sale_allocations
                 WHERE order_id = ? AND released_at IS NULL
                 FOR UPDATE
                """, (rs, row) -> new Allocation(rs.getString(1), rs.getInt(2)), orderId);
        for (Allocation allocation : allocations) {
            jdbcTemplate.update("""
                    UPDATE flash_sale_registrations
                       SET sold_count = GREATEST(0, sold_count - ?), version = version + 1,
                           updated_at = CURRENT_TIMESTAMP
                     WHERE registration_id = ?
                    """, allocation.quantity(), allocation.registrationId());
        }
        jdbcTemplate.update("""
                UPDATE flash_sale_allocations SET released_at = CURRENT_TIMESTAMP
                 WHERE order_id = ? AND released_at IS NULL
                """, orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, SkuFlashSale> findActiveBySkuIds(List<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }
        Map<String, SkuFlashSale> result = new HashMap<>();
        Map<String, PromotionCampaign> campaigns = new HashMap<>();
        for (FlashSaleRegistration registration : registrationRepository.findApprovedRunningBySkuIds(skuIds)) {
            if (!registration.hasStockLeft()) {
                continue;
            }
            PromotionCampaign campaign = campaigns.computeIfAbsent(registration.getCampaignId(),
                    id -> campaignRepository.findById(id).orElse(null));
            if (campaign == null || campaign.getStatus() != CampaignStatus.RUNNING) {
                continue;
            }
            result.put(registration.getSkuId(), new SkuFlashSale(
                    registration.getSkuId(),
                    registration.getRegistrationId(),
                    registration.getCampaignId(),
                    registration.getSalePrice().amount(),
                    registration.getSalePrice().currency(),
                    campaign.getEndDate(),
                    registration.getSaleStock() - registration.getSoldCount()));
        }
        return result;
    }

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
        campaignJpaRepository.findByTypeAndStatus(
                        CampaignType.FLASH_SALE.name(), CampaignStatus.RUNNING.name(), PageRequest.of(0, safe)).stream()
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
