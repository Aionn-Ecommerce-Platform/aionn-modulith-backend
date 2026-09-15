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
     * Append that tolerates its own retry.
     *
     * <p>{@code ON CONFLICT DO NOTHING} is what makes ingest idempotent under at-least-once outbox
     * delivery: a redelivered event carries the same {@code sourceEventId}, hits the partial unique
     * index, and is dropped by the database instead of adding a second row that every popularity and
     * affinity sum would then count twice. Returns the number of rows actually written, so the caller
     * can tell a fresh signal from a replay.
     *
     * <p>Written as a statement rather than {@code save()} because a JPA persist turns the conflict
     * into a {@code DataIntegrityViolationException} that aborts the surrounding transaction.
     */
    @Modifying
    @Query(value = """
            INSERT INTO recommendation_interactions
                (interaction_id, user_id, product_id, interaction_type, weight,
                 occurred_at, created_at, source_event_id)
            VALUES (:#{#entity.interactionId}, :#{#entity.userId}, :#{#entity.productId},
                    :#{#entity.interactionType}, :#{#entity.weight}, :#{#entity.occurredAt},
                    :#{#entity.createdAt}, :#{#entity.sourceEventId})
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
     * Users whose profile is stale relative to ingested activity, most recently ingested first.
     *
     * <p>Joining the profile table is what makes the sweep fair. Selecting the first N user IDs in
     * lexicographic order - which is what a plain {@code ORDER BY user_id LIMIT} does - returns the
     * same N users on every run, so once active users in a window exceed the batch size everyone past
     * that fixed prefix is never refreshed. Here a user leaves the result set as soon as their profile
     * covers their latest ingested interaction, which lets the batch rotate through the whole
     * population, and users with no profile row at all always qualify within the lookback.
     *
     * <p>Staleness uses ingestion time ({@code created_at}), not business time ({@code occurred_at}):
     * delayed events may arrive after a refresh even though they occurred before it. Business time
     * still determines which interactions fall within the lookback.
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
     *
     * <p>The cap is applied to the symmetric expansion and then intersected, so a pair is returned
     * only when it is in the top N of <em>both</em> endpoints. Capping one direction alone would not
     * bound the stored table, because the caller writes each returned pair both ways. Ties break on
     * the partner ID so repeated runs over unchanged data produce the same matrix.
     *
     * <p>The expansion CTE is named {@code expanded} rather than {@code symmetric} because the latter is
     * a reserved word in Postgres ({@code BETWEEN SYMMETRIC}), which the parser rejects in that position.
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

    /**
     * Serialises everything this module does to one user's behavioural data.
     *
     * <p>Held until the calling transaction ends, which is what makes the erasure check in ingest and in
     * the profile refresh authoritative rather than a race: without it, a refresh that read the user's
     * interactions before an erasure committed would write the profile straight back afterwards, and an
     * ingest that checked the mark before the erasure would append a row the erasure had already
     * finished deleting.
     *
     * <p>An advisory lock rather than a row lock because the thing being protected is often the absence
     * of a row. {@code hashtext} collisions between two user IDs are harmless - they only serialise two
     * unrelated users against each other for the length of one short transaction.
     *
     * <p>Returns a value only because Spring Data needs a result type for a native query; the lock
     * function itself returns {@code void}, so the expression is a constant.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(:userId)) IS NOT NULL", nativeQuery = true)
    boolean lockUser(@Param("userId") String userId);

    /**
     * Records that an account's behavioural data was erased, so a late event or a stale sweep cannot
     * recreate it. Idempotent: erasure is delivered at least once like every other integration event.
     */
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
