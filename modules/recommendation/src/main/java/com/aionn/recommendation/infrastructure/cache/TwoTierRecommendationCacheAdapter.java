package com.aionn.recommendation.infrastructure.cache;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.out.RecommendationCachePort;
import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import com.aionn.platform.infrastructure.cache.core.TwoTierCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Routes each surface to the cache whose TTL matches its data: similarity rebuilds hourly,
 * popularity and profiles every fifteen minutes. Hits/misses are counted per surface.
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
        // One shared entry; separate cache because its TTL follows the popularity rebuild.
        return load(trendingCache, RecommendationSurface.HOME, TRENDING_KEY, loader);
    }

    @Override
    public void evictUserFeed(String userId) {
        homeFeedCache.evict(userId);
    }

    @Override
    public void evictProduct(String productId) {
        similarProductsCache.evict(productId);
        similarProductsCache.evict("also-bought:" + productId);
    }

    @Override
    public void evictTrending() {
        trendingCache.evict(TRENDING_KEY);
    }

    /** Read-through with hit/miss attributed to the surface being served. */
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
     * Also-bought shares the similar-products cache and TTL. Cart also lands here, but the read path
     * never asks for it because cart suggestions are deliberately uncached.
     */
    private TwoTierCache<String, List<RecommendationItemResult>> cacheFor(
            RecommendationSurface surface) {
        return switch (surface) {
            case HOME -> homeFeedCache;
            case SIMILAR, ALSO_BOUGHT, CART -> similarProductsCache;
        };
    }
}
