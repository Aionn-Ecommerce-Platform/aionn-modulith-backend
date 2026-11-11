package com.aionn.recommendation.application.port.out;

import com.aionn.recommendation.domain.model.UserInteraction;
import com.aionn.recommendation.domain.valueobject.InteractionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InteractionPersistencePort {

    void append(UserInteraction interaction);

    List<UserInteraction> findByUser(String userId, Instant since, int limit);

    /** Product IDs the user already bought, used to keep them out of recommendations. */
    List<String> findPurchasedProductIds(String userId);

    /**
     * Users with activity ingested after their profile refresh, newest first.
     * Comparing against the profile keeps the sweep fair; a lexicographic prefix
     * would never move past the batch size.
     */
    List<String> findUserIdsWithInteractionsSince(Instant since, int limit);

    /** Decayed weight per product over the window; half-lives are passed in per type. */
    Map<String, PopularityAggregate> aggregatePopularity(
            Instant since, Instant now, Map<InteractionType, Long> halfLifeSeconds);

    /**
     * Item-to-item cosine similarity, computed in the database.
     * The neighbour cap bounds pair growth so popular products don't accumulate unread neighbours.
     */
    List<SimilarityRow> computeItemSimilarity(
            Instant since,
            Collection<InteractionType> strongTypes,
            int minCoOccurrence,
            int maxNeighboursPerProduct);

    int deleteOlderThan(Instant cutoff, int batchSize);

    /**
     * Deletes one bounded batch and reports the users/products whose behaviour was
     * removed, so derived profiles and recommendation signals can be invalidated
     * immediately instead of waiting for the next scheduled rebuild.
     */
    PrunedInteractions pruneOlderThan(Instant cutoff, int batchSize);

    int deleteByUser(String userId);

    /**
     * Takes a transaction-scoped lock on one user's behavioural data.
     * Erasure, ingest and profile refresh read-then-write the same rows; without the lock they race
     * and can undo each other. Must be called inside the transaction it protects.
     */
    void lockUser(String userId);

    /**
     * Records that an account's behavioural data was erased.
     * Rows alone can't distinguish an erased account from one that never had data, and at-least-once
     * delivery can write it back.
     */
    void markUserErased(String userId, Instant erasedAt);

    /** Whether this account's behavioural data has been erased. */
    boolean isUserErased(String userId);

    record PopularityAggregate(BigDecimal decayedScore, long viewCount, long purchaseCount) {
    }

    record SimilarityRow(String productId, String similarProductId, double score, int coOccurrence) {
    }

    record PrunedInteractions(int deleted, List<String> userIds, List<String> productIds) {
    }
}
