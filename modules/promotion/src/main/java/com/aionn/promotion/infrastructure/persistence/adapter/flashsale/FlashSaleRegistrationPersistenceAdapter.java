package com.aionn.promotion.infrastructure.persistence.adapter.flashsale;

import com.aionn.promotion.application.port.out.FlashSaleRegistrationPersistencePort;
import com.aionn.promotion.domain.model.FlashSaleRegistration;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;
import com.aionn.promotion.infrastructure.persistence.entity.FlashSaleRegistrationEntity;
import com.aionn.promotion.infrastructure.persistence.mapper.FlashSaleRegistrationDomainMapper;
import com.aionn.promotion.infrastructure.persistence.repository.FlashSaleRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FlashSaleRegistrationPersistenceAdapter implements FlashSaleRegistrationPersistencePort {

    private final FlashSaleRegistrationRepository jpa;
    private final FlashSaleRegistrationDomainMapper mapper;

    @Override
    public FlashSaleRegistration save(FlashSaleRegistration registration) {
        FlashSaleRegistrationEntity existing = jpa.findById(registration.getRegistrationId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(registration, existing)));
    }

    @Override
    public Optional<FlashSaleRegistration> findById(String registrationId) {
        return jpa.findById(registrationId).map(mapper::toDomain);
    }

    @Override
    public Optional<FlashSaleRegistration> findActiveBySkuAndCampaign(String campaignId, String skuId) {
        return jpa.findActiveBySkuAndCampaign(campaignId, skuId).map(mapper::toDomain);
    }

    @Override
    public List<FlashSaleRegistration> findByMerchant(String merchantId, FlashSaleRegistrationStatus status, int limit) {
        return jpa.findByMerchant(merchantId, status == null ? null : status.name(),
                PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<FlashSaleRegistration> findByCampaign(String campaignId, FlashSaleRegistrationStatus status, int limit) {
        return jpa.findByCampaign(campaignId, status == null ? null : status.name(),
                PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<FlashSaleRegistration> findByStatus(FlashSaleRegistrationStatus status, int limit) {
        return jpa.findByStatus(status.name(), PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<FlashSaleRegistration> findApprovedRunningBySkuIds(List<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        return jpa.findApprovedRunningBySkuIds(skuIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<FlashSaleRegistration> findApprovedRunningByProductIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return jpa.findApprovedRunningByProductIds(productIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<FlashSaleRegistration> findAllApprovedRunning(int limit) {
        return jpa.findAllApprovedRunning(PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
