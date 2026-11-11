package com.aionn.recommendation.application.port.out;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;

import java.util.List;
import java.util.function.Supplier;

/**
 * Caches ranked slates only; the caller applies stock availability afterwards, because inventory
 * changes far faster than any TTL here. A port because the cache implementation lives in infrastructure.
 */
public interface RecommendationCachePort {

    List<RecommendationItemResult> getOrLoad(
            RecommendationSurface surface,
            String key,
            Supplier<List<RecommendationItemResult>> loader);

    /** Trending is caller-independent: one shared entry, so a cold-start visitor never populates a per-user one. */
    List<RecommendationItemResult> getOrLoadTrending(
            Supplier<List<RecommendationItemResult>> loader);

    /** Drops a user's home feed after a signal strong enough to change it, such as a purchase. */
    void evictUserFeed(String userId);

    /** Drops similar/also-bought slates for a product after its signals changed. */
    void evictProduct(String productId);

    /** Drops the shared trending slate after popularity inputs changed. */
    void evictTrending();
}
