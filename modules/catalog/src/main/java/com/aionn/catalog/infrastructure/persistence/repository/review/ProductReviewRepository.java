package com.aionn.catalog.infrastructure.persistence.repository.review;

import com.aionn.catalog.infrastructure.persistence.entity.ProductReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReviewEntity, String> {

    boolean existsByUserIdAndProductId(String userId, String productId);

    Page<ProductReviewEntity> findByProductIdAndStatus(String productId, String status, Pageable pageable);

    long countByProductIdAndStatus(String productId, String status);

    Page<ProductReviewEntity> findByProductId(String productId, Pageable pageable);

    long countByProductId(String productId);

    Page<ProductReviewEntity> findByUserId(String userId, Pageable pageable);

    long countByUserId(String userId);

    @Query("SELECT r FROM ProductReviewEntity r WHERE r.productId IN "
            + "(SELECT p.productId FROM ProductEntity p WHERE p.merchantId = :merchantId) "
            + "AND (:replied IS NULL OR (:replied = true AND r.merchantReply IS NOT NULL) "
            + "OR (:replied = false AND r.merchantReply IS NULL))")
    Page<ProductReviewEntity> findByMerchantId(String merchantId, Boolean replied, Pageable pageable);

    @Query("SELECT COUNT(r) FROM ProductReviewEntity r WHERE r.productId IN "
            + "(SELECT p.productId FROM ProductEntity p WHERE p.merchantId = :merchantId) "
            + "AND (:replied IS NULL OR (:replied = true AND r.merchantReply IS NOT NULL) "
            + "OR (:replied = false AND r.merchantReply IS NULL))")
    long countByMerchantId(String merchantId, Boolean replied);

    Page<ProductReviewEntity> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM ProductReviewEntity r WHERE r.productId = :productId AND r.status = 'VISIBLE'")
    double getAverageRating(String productId);

    @Query("SELECT AVG(r.rating) FROM ProductReviewEntity r WHERE r.status = 'VISIBLE'")
    Double getPlatformAverageRating();

    @Query("SELECT COUNT(r) FROM ProductReviewEntity r WHERE r.status = 'VISIBLE'")
    long countAllVisibleReviews();

    @Query("SELECT r.rating, COUNT(r) FROM ProductReviewEntity r WHERE r.productId = :productId AND r.status = 'VISIBLE' GROUP BY r.rating")
    List<Object[]> countRatingsGroupByRating(String productId);
}
