package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.flashsale.command.FlashSaleCommands;
import com.aionn.promotion.application.dto.flashsale.result.ActiveFlashSaleResult;
import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import com.aionn.promotion.application.mapper.FlashSaleResultMapper;
import com.aionn.promotion.application.port.out.FlashSaleRegistrationPersistencePort;
import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.domain.valueobject.CampaignStatus;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.FlashSaleRegistration;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.integration.port.catalog.PricingQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FlashSaleService {

    private final FlashSaleRegistrationPersistencePort registrationRepository;
    private final PromotionCampaignPersistencePort campaignRepository;
    private final MerchantQueryPort merchantQueryPort;
    private final PricingQueryPort pricingQueryPort;
    private final FlashSaleResultMapper mapper;
    private final EventPublisher eventPublisher;

    public FlashSaleRegistrationResult register(FlashSaleCommands.RegisterFlashSale command) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(command.ownerId())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                        "No merchant registered for the authenticated user"));

        PromotionCampaign campaign = campaignRepository.findById(command.campaignId())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.CAMPAIGN_NOT_FOUND));
        if (campaign.getType() != CampaignType.FLASH_SALE) {
            throw new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                    "Campaign is not a flash-sale");
        }

        registrationRepository.findActiveBySkuAndCampaign(command.campaignId(), command.skuId())
                .ifPresent(existing -> {
                    throw new PromotionException(PromotionErrorCode.FLASH_SALE_DUPLICATE);
                });

        PricingQueryPort.SkuPricing pricing = pricingQueryPort
                .resolvePricing(List.of(command.skuId()))
                .get(command.skuId());
        if (pricing == null || !pricing.merchantId().equals(merchantId)) {
            throw new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                    "SKU not owned by merchant or not found");
        }

        Money salePrice = Money.of(command.salePrice(),
                command.currency() == null ? pricing.currency() : command.currency());
        FlashSaleRegistration reg = FlashSaleRegistration.submit(
                IdGenerator.ulid(),
                command.campaignId(),
                merchantId,
                command.productId(),
                command.skuId(),
                salePrice,
                command.saleStock());
        FlashSaleRegistration saved = registrationRepository.save(reg);
        eventPublisher.publish(reg.pullEvents());
        return mapper.toResult(saved);
    }

    public FlashSaleRegistrationResult approve(FlashSaleCommands.ApproveFlashSale command) {
        FlashSaleRegistration reg = required(command.registrationId());
        PricingQueryPort.SkuPricing pricing = pricingQueryPort
                .resolvePricing(List.of(reg.getSkuId()))
                .get(reg.getSkuId());
        Money variantPrice = pricing == null ? null
                : Money.of(pricing.price(), pricing.currency());
        reg.approve(command.adminId(), variantPrice);
        FlashSaleRegistration saved = registrationRepository.save(reg);
        eventPublisher.publish(reg.pullEvents());
        return mapper.toResult(saved);
    }

    public FlashSaleRegistrationResult reject(FlashSaleCommands.RejectFlashSale command) {
        FlashSaleRegistration reg = required(command.registrationId());
        reg.reject(command.adminId(), command.reason());
        FlashSaleRegistration saved = registrationRepository.save(reg);
        eventPublisher.publish(reg.pullEvents());
        return mapper.toResult(saved);
    }

    public FlashSaleRegistrationResult cancel(FlashSaleCommands.CancelFlashSale command) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(command.ownerId())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                        "No merchant registered for the authenticated user"));
        FlashSaleRegistration reg = required(command.registrationId());
        reg.cancel(merchantId);
        FlashSaleRegistration saved = registrationRepository.save(reg);
        eventPublisher.publish(reg.pullEvents());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public List<FlashSaleRegistrationResult> listByMerchant(String ownerId,
                                                             FlashSaleRegistrationStatus status,
                                                             int limit) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                        "No merchant registered for the authenticated user"));
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return registrationRepository.findByMerchant(merchantId, status, safeLimit).stream()
                .map(mapper::toResult).toList();
    }

    @Transactional(readOnly = true)
    public List<FlashSaleRegistrationResult> listByStatus(FlashSaleRegistrationStatus status, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return registrationRepository.findByStatus(status, safeLimit).stream()
                .map(mapper::toResult).toList();
    }

    @Transactional(readOnly = true)
    public FlashSaleRegistrationResult get(String registrationId) {
        return mapper.toResult(required(registrationId));
    }

    @Transactional(readOnly = true)
    public List<ActiveFlashSaleResult> listActive(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<FlashSaleRegistration> approved = registrationRepository.findAllApprovedRunning(safeLimit * 50);
        if (approved.isEmpty()) {
            return List.of();
        }
        java.util.Map<String, java.util.List<FlashSaleRegistration>> byCampaign = new java.util.LinkedHashMap<>();
        for (FlashSaleRegistration reg : approved) {
            byCampaign.computeIfAbsent(reg.getCampaignId(), k -> new java.util.ArrayList<>()).add(reg);
        }
        java.util.List<ActiveFlashSaleResult> out = new java.util.ArrayList<>();
        for (var entry : byCampaign.entrySet()) {
            PromotionCampaign campaign = campaignRepository.findById(entry.getKey()).orElse(null);
            if (campaign == null || campaign.getStatus() != CampaignStatus.RUNNING) {
                continue;
            }
            java.util.List<ActiveFlashSaleResult.Item> items = entry.getValue().stream()
                    .map(reg -> new ActiveFlashSaleResult.Item(
                            reg.getRegistrationId(), reg.getProductId(), reg.getSkuId(), reg.getMerchantId(),
                            reg.getSalePrice() == null ? BigDecimal.ZERO : reg.getSalePrice().amount(),
                            reg.getSalePrice() == null ? "VND" : reg.getSalePrice().currency(),
                            reg.getSaleStock(), reg.getSoldCount()))
                    .toList();
            out.add(new ActiveFlashSaleResult(
                    campaign.getCampaignId(), campaign.getName(),
                    campaign.getStartDate(), campaign.getEndDate(), items));
            if (out.size() >= safeLimit) {
                break;
            }
        }
        return out;
    }

    private FlashSaleRegistration required(String registrationId) {
        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.FLASH_SALE_NOT_FOUND));
    }
}
