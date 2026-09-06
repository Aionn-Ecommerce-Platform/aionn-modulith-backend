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

    /** Users with at least one interaction after {@code since}; drives the incremental refresh. */
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
     */
    List<SimilarityRow> computeItemSimilarity(
            Instant since, Collection<InteractionType> strongTypes, int minCoOccurrence);

    int deleteOlderThan(Instant cutoff, int batchSize);

    int deleteByUser(String userId);

    record PopularityAggregate(BigDecimal decayedScore, long viewCount, long purchaseCount) {
    }

    record SimilarityRow(String productId, String similarProductId, double score, int coOccurrence) {
    }
}
