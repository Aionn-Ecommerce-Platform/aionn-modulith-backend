package com.aionn.promotion.infrastructure.persistence.repository;

import com.aionn.promotion.infrastructure.persistence.entity.PromotionBannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaPromotionBannerRepository extends JpaRepository<PromotionBannerEntity, String> {

    @Query("SELECT b FROM PromotionBannerEntity b WHERE b.active = true ORDER BY b.displayOrder ASC")
    List<PromotionBannerEntity> findAllActiveOrderByDisplayOrder();

    @Query("SELECT b FROM PromotionBannerEntity b ORDER BY b.displayOrder ASC, b.createdAt ASC")
    List<PromotionBannerEntity> findAllOrdered();
}
