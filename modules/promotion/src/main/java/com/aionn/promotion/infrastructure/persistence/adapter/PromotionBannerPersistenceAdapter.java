package com.aionn.promotion.infrastructure.persistence.adapter;

import com.aionn.promotion.application.port.out.PromotionBannerPersistencePort;
import com.aionn.promotion.domain.model.PromotionBanner;
import com.aionn.promotion.application.dto.common.PageResult;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import com.aionn.promotion.infrastructure.persistence.entity.PromotionBannerEntity;
import com.aionn.promotion.infrastructure.persistence.mapper.PromotionBannerDomainMapper;
import com.aionn.promotion.infrastructure.persistence.repository.JpaPromotionBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.domain.PageRequest;

@Repository
@RequiredArgsConstructor
public class PromotionBannerPersistenceAdapter implements PromotionBannerPersistencePort {

    private final JpaPromotionBannerRepository jpa;
    private final PromotionBannerDomainMapper mapper;

    @Override
    public PageResult<PromotionBanner> findAllActive(OffsetPagination pagination) {
        var page = jpa.findAllActiveOrderByDisplayOrder(PageRequest.of(pagination.page(), pagination.size()))
                .map(mapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public PageResult<PromotionBanner> findAll(OffsetPagination pagination) {
        var page = jpa.findAllOrdered(PageRequest.of(pagination.page(), pagination.size()))
                .map(mapper::toDomain);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
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
