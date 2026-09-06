package com.aionn.recommendation.infrastructure.cache;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.out.RecommendationCachePort;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Routes each surface to the cache whose TTL matches the refresh cadence of the data behind it:
 * similarity is rebuilt hourly, popularity and profiles every fifteen minutes.
 */
@Component
public class TwoTierRecommendationCacheAdapter implements RecommendationCachePort {

    private final TwoTierCache<String, List<RecommendationItemResult>> homeFeedCache;
    private final TwoTierCache<String, List<RecommendationItemResult>> similarProductsCache;
    private final TwoTierCache<String, List<RecommendationItemResult>> trendingCache;

    public TwoTierRecommendationCacheAdapter(
            @Qualifier("recommendationHomeFeedCache")
            TwoTierCache<String, List<RecommendationItemResult>> homeFeedCache,
            @Qualifier("recommendationSimilarProductsCache")
            TwoTierCache<String, List<RecommendationItemResult>> similarProductsCache,
            @Qualifier("recommendationTrendingCache")
            TwoTierCache<String, List<RecommendationItemResult>> trendingCache) {
        this.homeFeedCache = homeFeedCache;
        this.similarProductsCache = similarProductsCache;
        this.trendingCache = trendingCache;
    }

    @Override
    public List<RecommendationItemResult> getOrLoad(
            RecommendationSurface surface, String key, Supplier<List<RecommendationItemResult>> loader) {
        return cacheFor(surface).getOrLoad(key, loader);
    }

    @Override
    public List<RecommendationItemResult> getOrLoadTrending(
            Supplier<List<RecommendationItemResult>> loader) {
        // One entry for everyone: the slate has no per-user component.
        return trendingCache.getOrLoad("trending", loader);
    }

    @Override
    public void evictUserFeed(String userId) {
        homeFeedCache.evict(userId);
    }

    private TwoTierCache<String, List<RecommendationItemResult>> cacheFor(
            RecommendationSurface surface) {
        return switch (surface) {
            case HOME -> homeFeedCache;
            case SIMILAR, ALSO_BOUGHT, CART -> similarProductsCache;
        };
    }
}
