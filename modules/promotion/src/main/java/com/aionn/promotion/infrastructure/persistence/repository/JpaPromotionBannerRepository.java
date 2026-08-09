package com.aionn.promotion.infrastructure.persistence.repository;

import com.aionn.promotion.infrastructure.persistence.entity.PromotionBannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JpaPromotionBannerRepository extends JpaRepository<PromotionBannerEntity, String> {

    @Query("SELECT b FROM PromotionBannerEntity b WHERE b.active = true ORDER BY b.displayOrder ASC, b.bannerId ASC")
    Page<PromotionBannerEntity> findAllActiveOrderByDisplayOrder(Pageable pageable);

    @Query("SELECT b FROM PromotionBannerEntity b ORDER BY b.displayOrder ASC, b.createdAt ASC, b.bannerId ASC")
    Page<PromotionBannerEntity> findAllOrdered(Pageable pageable);
}
