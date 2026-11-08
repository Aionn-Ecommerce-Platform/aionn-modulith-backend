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

    /**
     * Append that tolerates at-least-once redelivery (same {@code sourceEventId} is dropped).
     * Returns rows written, so the caller can tell a fresh signal from a replay. Must be a native
     * statement: a JPA persist would turn the conflict into an exception that aborts the transaction.
     */
    @Modifying
    @Query(value = """
            INSERT INTO recommendation_interactions
                (interaction_id, user_id, product_id, interaction_type, weight,
                 occurred_at, created_at, source_event_id, category_ids, brand_id, keyword)
            VALUES (:#{#entity.interactionId}, :#{#entity.userId}, :#{#entity.productId},
                    :#{#entity.interactionType}, :#{#entity.weight}, :#{#entity.occurredAt},
                    :#{#entity.createdAt}, :#{#entity.sourceEventId},
                    CAST(:#{#entity.categoryIdsJson} AS jsonb), :#{#entity.brandId}, :#{#entity.keyword})
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int appendIdempotent(@Param("entity") InteractionEntity entity);

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

    /**
     * Users whose profile is stale relative to ingested activity, newest first. Joining the profile
     * table keeps the batch rotating: a plain {@code ORDER BY user_id LIMIT} would return the same
     * users every run and starve the rest. Staleness uses {@code created_at} (ingestion time) because
     * events can arrive late; {@code occurred_at} still bounds the lookback window.
     */
    @Query(value = """
            SELECT i.user_id
            FROM recommendation_interactions i
            LEFT JOIN recommendation_user_profiles p ON p.user_id = i.user_id
            WHERE i.occurred_at >= :since
            GROUP BY i.user_id, p.refreshed_at
            HAVING MAX(i.created_at) > COALESCE(p.refreshed_at, '-infinity'::timestamptz)
            ORDER BY MAX(i.created_at) DESC, i.user_id
            LIMIT :limit
            """, nativeQuery = true)
    List<String> findUserIdsWithInteractionsSince(
            @Param("since") Instant since, @Param("limit") int limit);

    /** Sums decayed weights ({@code 2^(-age/half_life)}) per product so recent sales outrank old ones. */
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
              AND product_id IS NOT NULL
            GROUP BY product_id
            """, nativeQuery = true)
    List<PopularityProjection> aggregatePopularity(
            @Param("since") Instant since,
            @Param("now") Instant now,
            @Param("viewHalfLife") long viewHalfLifeSeconds,
            @Param("cartAddHalfLife") long cartAddHalfLifeSeconds,
            @Param("purchaseHalfLife") long purchaseHalfLifeSeconds);

    /**
     * Item-to-item cosine similarity over users who interacted strongly with both products.
     * {@code a.product_id < b.product_id} yields each unordered pair once; the cap must be applied to
     * the symmetric expansion (then intersected) or the stored table is unbounded, because callers
     * write each returned pair both ways. Ties break on partner ID for reproducible runs.
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
            ),
            scored AS (
                SELECT p.product_id,
                       p.similar_product_id,
                       p.co_occurrence,
                       (p.co_occurrence / SQRT(CAST(c1.user_count AS double precision)
                                               * CAST(c2.user_count AS double precision))) AS score
                FROM pairs p
                JOIN counts c1 ON c1.product_id = p.product_id
                JOIN counts c2 ON c2.product_id = p.similar_product_id
            ),
            expanded AS (
                SELECT product_id, similar_product_id, score FROM scored
                UNION ALL
                SELECT similar_product_id AS product_id, product_id AS similar_product_id, score
                FROM scored
            ),
            kept AS (
                SELECT product_id, similar_product_id
                FROM (
                    SELECT e.product_id,
                           e.similar_product_id,
                           ROW_NUMBER() OVER (
                               PARTITION BY e.product_id
                               ORDER BY e.score DESC, e.similar_product_id) AS neighbour_rank
                    FROM expanded e
                ) ranked
                WHERE ranked.neighbour_rank <= :maxNeighboursPerProduct
            )
            SELECT s.product_id         AS productId,
                   s.similar_product_id AS similarProductId,
                   s.co_occurrence      AS coOccurrence,
                   s.score              AS score
            FROM scored s
            WHERE EXISTS (SELECT 1 FROM kept k
                          WHERE k.product_id = s.product_id
                            AND k.similar_product_id = s.similar_product_id)
              AND EXISTS (SELECT 1 FROM kept k
                          WHERE k.product_id = s.similar_product_id
                            AND k.similar_product_id = s.product_id)
            """, nativeQuery = true)
    List<SimilarityProjection> computeItemSimilarity(
            @Param("since") Instant since,
            @Param("strongTypes") Collection<String> strongTypes,
            @Param("minCoOccurrence") int minCoOccurrence,
            @Param("maxNeighboursPerProduct") int maxNeighboursPerProduct);

    /** Deletes in bounded batches so the sweep doesn't hold a long lock on a concurrently-written table. */
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

    /**
     * Serialises all work on one user's behavioural data until the transaction ends, making the erasure
     * checks in ingest and profile refresh authoritative. An advisory lock, not a row lock, because the
     * protected thing is often the absence of a row; hashtext collisions only serialise two unrelated
     * users for one short transaction. Returns a constant because Spring Data needs a result type.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(:userId)) IS NOT NULL", nativeQuery = true)
    boolean lockUser(@Param("userId") String userId);

    /** Erasure tombstone so a late event or stale sweep can't recreate the data. Idempotent. */
    @Modifying
    @Query(value = """
            INSERT INTO recommendation_erased_users (user_id, erased_at)
            VALUES (:userId, :erasedAt)
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    int markUserErased(@Param("userId") String userId, @Param("erasedAt") Instant erasedAt);

    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM recommendation_erased_users WHERE user_id = :userId)
            """, nativeQuery = true)
    boolean isUserErased(@Param("userId") String userId);

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
