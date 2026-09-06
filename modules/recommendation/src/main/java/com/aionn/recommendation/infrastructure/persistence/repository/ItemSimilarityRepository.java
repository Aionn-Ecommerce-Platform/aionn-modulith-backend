package com.aionn.recommendation.infrastructure.persistence.repository;

import com.aionn.recommendation.infrastructure.persistence.entity.ItemSimilarityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ItemSimilarityRepository
        extends JpaRepository<ItemSimilarityEntity, ItemSimilarityEntity.ItemSimilarityId> {

    @Query(value = """
            SELECT * FROM recommendation_item_similarity
            WHERE product_id = :productId
            ORDER BY score DESC, similar_product_id
            LIMIT :limit
            """, nativeQuery = true)
    List<ItemSimilarityEntity> findNeighbours(
            @Param("productId") String productId, @Param("limit") int limit);

    /**
     * Top neighbours per seed product in one round trip. A lateral join keeps the per-product limit -
     * a plain {@code IN} with a global limit would let one popular seed crowd out every other.
     */
    @Query(value = """
            SELECT s.* FROM unnest(CAST(:productIds AS text[])) AS seed(product_id)
            JOIN LATERAL (
                SELECT * FROM recommendation_item_similarity sim
                WHERE sim.product_id = seed.product_id
                ORDER BY sim.score DESC, sim.similar_product_id
                LIMIT :limitPerProduct
            ) s ON TRUE
            """, nativeQuery = true)
    List<ItemSimilarityEntity> findNeighboursForAll(
            @Param("productIds") String[] productIds,
            @Param("limitPerProduct") int limitPerProduct);

    @Modifying
    @Query(value = """
            INSERT INTO recommendation_item_similarity
                (product_id, similar_product_id, score, co_occurrence, computed_at)
            VALUES (:productId, :similarProductId, :score, :coOccurrence, :computedAt)
            ON CONFLICT (product_id, similar_product_id) DO UPDATE SET
                score         = EXCLUDED.score,
                co_occurrence = EXCLUDED.co_occurrence,
                computed_at   = EXCLUDED.computed_at
            """, nativeQuery = true)
    void upsert(
            @Param("productId") String productId,
            @Param("similarProductId") String similarProductId,
            @Param("score") java.math.BigDecimal score,
            @Param("coOccurrence") int coOccurrence,
            @Param("computedAt") Instant computedAt);

    @Modifying
    @Query(value = "DELETE FROM recommendation_item_similarity WHERE computed_at < :cutoff",
            nativeQuery = true)
    int deleteComputedBefore(@Param("cutoff") Instant cutoff);

    List<ItemSimilarityEntity> findByIdProductIdIn(Collection<String> productIds);
}
