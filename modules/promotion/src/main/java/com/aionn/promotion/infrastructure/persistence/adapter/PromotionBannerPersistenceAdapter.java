package com.aionn.promotion.infrastructure.persistence.adapter;

import com.aionn.promotion.application.port.out.PromotionBannerPersistencePort;
import com.aionn.promotion.domain.model.PromotionBanner;
import com.aionn.promotion.infrastructure.persistence.entity.PromotionBannerEntity;
import com.aionn.promotion.infrastructure.persistence.mapper.PromotionBannerDomainMapper;
import com.aionn.promotion.infrastructure.persistence.repository.JpaPromotionBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PromotionBannerPersistenceAdapter implements PromotionBannerPersistencePort {

    private final JpaPromotionBannerRepository jpa;
    private final PromotionBannerDomainMapper mapper;

    @Override
    public List<PromotionBanner> findAllActive() {
        return jpa.findAllActiveOrderByDisplayOrder().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PromotionBanner> findAll() {
        return jpa.findAllOrdered().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PromotionBanner> findById(String bannerId) {
        return jpa.findById(bannerId).map(mapper::toDomain);
    }

    @Override
    public PromotionBanner save(PromotionBanner banner) {
        PromotionBannerEntity existing = jpa.findById(banner.getBannerId()).orElse(null);
        PromotionBannerEntity entity = mapper.toEntity(banner, existing);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public void deleteById(String bannerId) {
        jpa.deleteById(bannerId);
    }
}
