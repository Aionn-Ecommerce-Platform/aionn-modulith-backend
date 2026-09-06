package com.aionn.recommendation.application.port.out;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;

import java.util.List;
import java.util.function.Supplier;

/**
 * Caches ranked slates.
 *
 * <p>An application port rather than a direct cache dependency: the two-tier cache lives in
 * infrastructure, and application code may not reach into that layer.
 *
 * <p>What is cached is the ranked slate only. Stock availability is applied by the caller after this
 * returns, because inventory changes far faster than any TTL here and a cached availability decision
 * would serve unbuyable products for the life of the entry.
 */
public interface RecommendationCachePort {

    List<RecommendationItemResult> getOrLoad(
            RecommendationSurface surface,
            String key,
            Supplier<List<RecommendationItemResult>> loader);

    /**
     * Trending is identical for every caller, so it gets one shared entry rather than one per user.
     * Keeping it separate also means a cold-start visitor never populates a per-user entry.
     */
    List<RecommendationItemResult> getOrLoadTrending(
            Supplier<List<RecommendationItemResult>> loader);

    /** Drops a user's home feed after a signal strong enough to change it, such as a purchase. */
    void evictUserFeed(String userId);
}
