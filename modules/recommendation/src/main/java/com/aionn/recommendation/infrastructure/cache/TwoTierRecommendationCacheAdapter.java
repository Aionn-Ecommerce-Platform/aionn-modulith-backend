package com.aionn.recommendation.infrastructure.cache;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.out.RecommendationCachePort;
import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Routes each surface to the cache whose TTL matches the refresh cadence of the data behind it:
 * similarity is rebuilt hourly, popularity and profiles every fifteen minutes.
 *
 * <p>Hit and miss are counted here rather than left to the cache implementation because the useful
 * dimension is the surface, not the cache instance: similar and also-bought share one cache but are
 * separate products with separate hit rates.
 */
@Component
public class TwoTierRecommendationCacheAdapter implements RecommendationCachePort {

    /** Single key for the shared trending slate, which has no per-user or per-product component. */
    private static final String TRENDING_KEY = "trending";

    private final TwoTierCache<String, List<RecommendationItemResult>> homeFeedCache;
    private final TwoTierCache<String, List<RecommendationItemResult>> similarProductsCache;
    private final TwoTierCache<String, List<RecommendationItemResult>> trendingCache;
    private final RecommendationMetricsPort metrics;

    public TwoTierRecommendationCacheAdapter(
            @Qualifier("recommendationHomeFeedCache")
            TwoTierCache<String, List<RecommendationItemResult>> homeFeedCache,
            @Qualifier("recommendationSimilarProductsCache")
            TwoTierCache<String, List<RecommendationItemResult>> similarProductsCache,
            @Qualifier("recommendationTrendingCache")
            TwoTierCache<String, List<RecommendationItemResult>> trendingCache,
            RecommendationMetricsPort metrics) {
        this.homeFeedCache = homeFeedCache;
        this.similarProductsCache = similarProductsCache;
        this.trendingCache = trendingCache;
        this.metrics = metrics;
    }

    @Override
    public List<RecommendationItemResult> getOrLoad(
            RecommendationSurface surface, String key, Supplier<List<RecommendationItemResult>> loader) {
        return load(cacheFor(surface), surface, key, loader);
    }

    @Override
    public List<RecommendationItemResult> getOrLoadTrending(
            Supplier<List<RecommendationItemResult>> loader) {
        // One entry for everyone: the slate has no per-user component. Kept on its own cache rather
        // than the home feed's because its TTL follows the popularity rebuild, not the per-user feed,
        // and because a cold-start visitor should not populate a per-user entry.
        return load(trendingCache, RecommendationSurface.HOME, TRENDING_KEY, loader);
    }

    @Override
    public void evictUserFeed(String userId) {
        homeFeedCache.evict(userId);
    }

    /**
     * Read-through with the hit or miss attributed to the surface being served, which is the dimension
     * worth graphing: similar and also-bought share one cache instance but are separate products.
     */
    private List<RecommendationItemResult> load(
            TwoTierCache<String, List<RecommendationItemResult>> cache,
            RecommendationSurface surface,
            String key,
            Supplier<List<RecommendationItemResult>> loader) {
        Optional<List<RecommendationItemResult>> cached = cache.get(key);
        if (cached.isPresent()) {
            metrics.recordCacheHit(surface);
            return cached.get();
        }
        metrics.recordCacheMiss(surface);
        List<RecommendationItemResult> loaded = loader.get();
        if (loaded != null) {
            cache.put(key, loaded);
        }
        return loaded;
    }

    /**
     * Also-bought shares the similar-products cache and its TTL, which is set by the hourly similarity
     * rebuild that feeds both. The two cannot collide because the caller prefixes also-bought keys.
     *
     * <p>Cart lands here too, for want of a better TTL, but the read path never asks for it: cart
     * suggestions are deliberately uncached, because the basket is the input and a key over a set of SKUs
     * would almost never repeat between users. Routing it rather than throwing keeps this port usable if
     * that decision is ever revisited.
     */
    private TwoTierCache<String, List<RecommendationItemResult>> cacheFor(
            RecommendationSurface surface) {
        return switch (surface) {
            case HOME -> homeFeedCache;
            case SIMILAR, ALSO_BOUGHT, CART -> similarProductsCache;
        };
    }
}
