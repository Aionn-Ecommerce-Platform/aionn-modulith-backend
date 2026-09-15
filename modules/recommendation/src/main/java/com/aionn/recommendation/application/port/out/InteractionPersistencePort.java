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
     * Users whose profile is stale relative to ingested activity, most recently ingested first.
     *
     * <p>Stale means an interaction within the business-time lookback was ingested after the profile
     * was last refreshed, or the user has no profile row at all. Comparing against the profile rather
     * than returning the first {@code limit} user IDs is what keeps the sweep fair: a lexicographic
     * prefix is the same set of users on every run, so once activity exceeds the batch size everyone
     * past that prefix is never refreshed.
     */
    List<String> findUserIdsWithInteractionsSince(Instant since, int limit);

    /**
     * Decayed weight per product, summed over the window. Half-lives are passed in seconds per type
     * so the decay curve stays configurable without the persistence layer knowing about policies.
     */
    Map<String, PopularityAggregate> aggregatePopularity(
            Instant since, Instant now, Map<InteractionType, Long> halfLifeSeconds);

    /**
     * Item-to-item cosine similarity over users who interacted strongly with both products.
     * Computed in the database because the pairwise join is far cheaper there than streaming every
     * interaction into the JVM.
     *
     * @param maxNeighboursPerProduct caps how many partners each product keeps, applied symmetrically
     *                                so both directions of a retained pair survive. Without the cap the
     *                                pair count grows with the square of catalogue activity and a
     *                                popular product accumulates neighbours nobody will ever read past.
     */
    List<SimilarityRow> computeItemSimilarity(
            Instant since,
            Collection<InteractionType> strongTypes,
            int minCoOccurrence,
            int maxNeighboursPerProduct);

    int deleteOlderThan(Instant cutoff, int batchSize);

    int deleteByUser(String userId);

    /**
     * Takes a transaction-scoped lock on one user's behavioural data.
     *
     * <p>Erasure, ingest and profile refresh all read-then-write the same user's rows, and two of them
     * running concurrently undo each other: a refresh that read the interactions before an erasure
     * committed writes the profile straight back, and an ingest that checked for an erasure mark before
     * it landed appends a row the erasure has already finished deleting. Holding this lock across each
     * of those transactions makes the erasure check decisive instead of a race.
     *
     * <p>Must be called inside the transaction whose work it is protecting; the lock is released when
     * that transaction ends.
     */
    void lockUser(String userId);

    /**
     * Records that an account's behavioural data was erased.
     *
     * <p>Deleting the rows is not enough on its own, because afterwards there is nothing to distinguish
     * an erased account from one that never had any behavioural data - and at-least-once delivery plus a
     * sweep that read its user IDs before the erasure will both try to write that data back.
     */
    void markUserErased(String userId, Instant erasedAt);

    /** Whether this account's behavioural data has been erased. */
    boolean isUserErased(String userId);

    record PopularityAggregate(BigDecimal decayedScore, long viewCount, long purchaseCount) {
    }

    record SimilarityRow(String productId, String similarProductId, double score, int coOccurrence) {
    }
}
