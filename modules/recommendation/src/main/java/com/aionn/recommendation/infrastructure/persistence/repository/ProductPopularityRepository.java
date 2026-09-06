package com.aionn.recommendation.infrastructure.persistence.repository;

import com.aionn.recommendation.infrastructure.persistence.entity.ProductPopularityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ProductPopularityRepository
        extends JpaRepository<ProductPopularityEntity, String> {

    @Query(value = """
            SELECT * FROM recommendation_popularity
            ORDER BY popularity_score DESC, product_id
            LIMIT :limit
            """, nativeQuery = true)
    List<ProductPopularityEntity> findTop(@Param("limit") int limit);

    List<ProductPopularityEntity> findByProductIdIn(Collection<String> productIds);

    /**
     * Upsert rather than insert: the refresh job rewrites the whole active set each run, and a
     * truncate-then-insert would leave readers with an empty table mid-rebuild.
     */
    @Modifying
    @Query(value = """
            INSERT INTO recommendation_popularity
                (product_id, popularity_score, view_count, purchase_count, computed_at)
            VALUES (:productId, :score, :viewCount, :purchaseCount, :computedAt)
            ON CONFLICT (product_id) DO UPDATE SET
                popularity_score = EXCLUDED.popularity_score,
                view_count       = EXCLUDED.view_count,
                purchase_count   = EXCLUDED.purchase_count,
                computed_at      = EXCLUDED.computed_at
            """, nativeQuery = true)
    void upsert(
            @Param("productId") String productId,
            @Param("score") java.math.BigDecimal score,
            @Param("viewCount") long viewCount,
            @Param("purchaseCount") long purchaseCount,
            @Param("computedAt") Instant computedAt);

    @Modifying
    @Query(value = "DELETE FROM recommendation_popularity WHERE computed_at < :cutoff",
            nativeQuery = true)
    int deleteComputedBefore(@Param("cutoff") Instant cutoff);
}
