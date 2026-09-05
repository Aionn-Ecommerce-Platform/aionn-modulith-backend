package com.aionn.promotion.infrastructure.persistence.repository;

import com.aionn.promotion.infrastructure.persistence.entity.PromotionBannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface JpaPromotionBannerRepository extends JpaRepository<PromotionBannerEntity, String> {

    @Query("SELECT b FROM PromotionBannerEntity b WHERE b.active = true ORDER BY b.displayOrder ASC, b.bannerId ASC")
    Page<PromotionBannerEntity> findAllActiveOrderByDisplayOrder(Pageable pageable);

    @Query("SELECT b FROM PromotionBannerEntity b ORDER BY b.displayOrder ASC, b.createdAt ASC, b.bannerId ASC")
    Page<PromotionBannerEntity> findAllOrdered(Pageable pageable);

    @Query(value = "SELECT (pg_advisory_xact_lock(hashtext('promotion_banner_display_order_seq')) IS NOT NULL AND false)::int + nextval('promotion_banner_display_order_seq')::int", nativeQuery = true)
    int nextDisplayOrder();

    @Query(value = "SELECT (pg_advisory_xact_lock(hashtext('promotion_banner_display_order_seq')) IS NOT NULL AND false)::int + setval('promotion_banner_display_order_seq', GREATEST(COALESCE((SELECT last_value FROM promotion_banner_display_order_seq), 1), :displayOrder), true)::int", nativeQuery = true)
    int advanceDisplayOrderSequence(@Param("displayOrder") int displayOrder);
}
