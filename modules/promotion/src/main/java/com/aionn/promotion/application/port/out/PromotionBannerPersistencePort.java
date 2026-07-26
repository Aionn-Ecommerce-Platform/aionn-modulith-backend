package com.aionn.promotion.application.port.out;

import com.aionn.promotion.domain.model.PromotionBanner;

import java.util.List;
import java.util.Optional;

public interface PromotionBannerPersistencePort {

    List<PromotionBanner> findAllActive();

    List<PromotionBanner> findAll();

    Optional<PromotionBanner> findById(String bannerId);

    PromotionBanner save(PromotionBanner banner);

    void deleteById(String bannerId);
}
