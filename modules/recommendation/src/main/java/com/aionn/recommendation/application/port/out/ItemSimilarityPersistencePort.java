package com.aionn.recommendation.application.port.out;

import com.aionn.recommendation.domain.model.ItemSimilarity;

import java.util.Collection;
import java.util.List;

public interface ItemSimilarityPersistencePort {

    List<ItemSimilarity> findNeighbours(String productId, int limit);

    /** Neighbours of several seed products at once, for home-feed and cart candidate generation. */
    List<ItemSimilarity> findNeighbours(Collection<String> productIds, int limitPerProduct);

    /**
     * Upserts a batch. Deliberately not a truncate-and-insert: readers would otherwise see an empty
     * table for the duration of the rebuild.
     */
    void upsertAll(List<ItemSimilarity> similarities);

    /** Removes pairs not refreshed by the latest run, so stale co-occurrences do not linger. */
    int deleteComputedBefore(java.time.Instant cutoff);
}
