package com.aionn.promotion.application.port.out;

import com.aionn.promotion.domain.model.PromotionBanner;
import com.aionn.promotion.application.dto.common.PageResult;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.util.Optional;

public interface PromotionBannerPersistencePort {

    PageResult<PromotionBanner> findAllActive(OffsetPagination pagination);

    PageResult<PromotionBanner> findAll(OffsetPagination pagination);

    Optional<PromotionBanner> findById(String bannerId);

    PromotionBanner save(PromotionBanner banner);

    void deleteById(String bannerId);
}
