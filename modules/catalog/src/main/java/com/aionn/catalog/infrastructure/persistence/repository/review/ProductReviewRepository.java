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

    Page<ProductReviewEntity> findByUserId(String userId, Pageable pageable);

    long countByUserId(String userId);

    Page<ProductReviewEntity> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM ProductReviewEntity r WHERE r.productId = :productId AND r.status = 'VISIBLE'")
    double getAverageRating(String productId);

    @Query("SELECT r.rating, COUNT(r) FROM ProductReviewEntity r WHERE r.productId = :productId AND r.status = 'VISIBLE' GROUP BY r.rating")
    List<Object[]> countRatingsGroupByRating(String productId);
}
