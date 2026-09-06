package com.aionn.recommendation.infrastructure.persistence.repository;

import com.aionn.recommendation.infrastructure.persistence.entity.InteractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface InteractionRepository extends JpaRepository<InteractionEntity, String> {

    @Query(value = """
            SELECT * FROM recommendation_interactions
            WHERE user_id = :userId
              AND occurred_at >= :since
            ORDER BY occurred_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<InteractionEntity> findByUserSince(
            @Param("userId") String userId,
            @Param("since") Instant since,
            @Param("limit") int limit);

    @Query(value = """
            SELECT DISTINCT product_id FROM recommendation_interactions
            WHERE user_id = :userId
              AND interaction_type = 'PURCHASE'
            """, nativeQuery = true)
    List<String> findPurchasedProductIds(@Param("userId") String userId);

    @Query(value = """
            SELECT DISTINCT user_id FROM recommendation_interactions
            WHERE occurred_at >= :since
            ORDER BY user_id
            LIMIT :limit
            """, nativeQuery = true)
    List<String> findUserIdsWithInteractionsSince(
            @Param("since") Instant since, @Param("limit") int limit);

    /**
     * Sums each interaction's weight after applying exponential decay, so a product that sold well
     * last week outranks one that sold well six months ago.
     *
     * <p>{@code POWER(2, -age / half_life)} is evaluated per row in the database rather than by
     * streaming every interaction into the JVM.
     */
    @Query(value = """
            SELECT product_id                                                   AS productId,
                   SUM(weight * POWER(
                       2,
                       -(EXTRACT(EPOCH FROM (CAST(:now AS timestamptz) - occurred_at))
                         / CASE interaction_type
                               WHEN 'VIEW'     THEN CAST(:viewHalfLife AS double precision)
                               WHEN 'CART_ADD' THEN CAST(:cartAddHalfLife AS double precision)
                               ELSE                 CAST(:purchaseHalfLife AS double precision)
                           END)))                                               AS decayedScore,
                   COUNT(*) FILTER (WHERE interaction_type = 'VIEW')            AS viewCount,
                   COUNT(*) FILTER (WHERE interaction_type = 'PURCHASE')        AS purchaseCount
            FROM recommendation_interactions
            WHERE occurred_at >= :since
            GROUP BY product_id
            """, nativeQuery = true)
    List<PopularityProjection> aggregatePopularity(
            @Param("since") Instant since,
            @Param("now") Instant now,
            @Param("viewHalfLife") long viewHalfLifeSeconds,
            @Param("cartAddHalfLife") long cartAddHalfLifeSeconds,
            @Param("purchaseHalfLife") long purchaseHalfLifeSeconds);

    /**
     * Item-to-item cosine similarity over users who interacted strongly with both products:
     * {@code |U_a n U_b| / sqrt(|U_a| * |U_b|)}.
     *
     * <p>{@code a.product_id < b.product_id} yields each unordered pair once. The
     * {@code minCoOccurrence} floor removes pairs that share a single user, where cosine is high by
     * arithmetic but meaningless as a signal.
     */
    @Query(value = """
            WITH strong AS (
                SELECT DISTINCT user_id, product_id
                FROM recommendation_interactions
                WHERE interaction_type IN (:strongTypes)
                  AND occurred_at >= :since
            ),
            counts AS (
                SELECT product_id, COUNT(DISTINCT user_id) AS user_count
                FROM strong
                GROUP BY product_id
            ),
            pairs AS (
                SELECT a.product_id AS product_id,
                       b.product_id AS similar_product_id,
                       COUNT(*)     AS co_occurrence
                FROM strong a
                JOIN strong b ON a.user_id = b.user_id AND a.product_id < b.product_id
                GROUP BY a.product_id, b.product_id
                HAVING COUNT(*) >= :minCoOccurrence
            )
            SELECT p.product_id         AS productId,
                   p.similar_product_id AS similarProductId,
                   p.co_occurrence      AS coOccurrence,
                   (p.co_occurrence / SQRT(CAST(c1.user_count AS double precision)
                                           * CAST(c2.user_count AS double precision))) AS score
            FROM pairs p
            JOIN counts c1 ON c1.product_id = p.product_id
            JOIN counts c2 ON c2.product_id = p.similar_product_id
            """, nativeQuery = true)
    List<SimilarityProjection> computeItemSimilarity(
            @Param("since") Instant since,
            @Param("strongTypes") Collection<String> strongTypes,
            @Param("minCoOccurrence") int minCoOccurrence);

    /**
     * Deletes in bounded batches so the sweep never holds a long lock on a table that the ingest
     * listener writes to continuously.
     */
    @Modifying
    @Query(value = """
            DELETE FROM recommendation_interactions
            WHERE interaction_id IN (
                SELECT interaction_id FROM recommendation_interactions
                WHERE occurred_at < :cutoff
                ORDER BY occurred_at
                LIMIT :batchSize
            )
            """, nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);

    @Modifying
    @Query(value = "DELETE FROM recommendation_interactions WHERE user_id = :userId", nativeQuery = true)
    int deleteByUserId(@Param("userId") String userId);

    interface PopularityProjection {
        String getProductId();

        java.math.BigDecimal getDecayedScore();

        long getViewCount();

        long getPurchaseCount();
    }

    interface SimilarityProjection {
        String getProductId();

        String getSimilarProductId();

        double getScore();

        int getCoOccurrence();
    }
}
