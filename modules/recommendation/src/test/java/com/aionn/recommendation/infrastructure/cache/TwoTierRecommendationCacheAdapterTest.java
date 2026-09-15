package com.aionn.recommendation.infrastructure.cache;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The routing is the whole adapter, and getting it wrong is silent: a slate would still be served, just
 * with the wrong TTL behind it.
 *
 * <p>The adapter reads through {@code get}/{@code put} rather than delegating to the cache's own
 * {@code getOrLoad} because the hit or miss has to be attributed to the surface being served, and the
 * cache implementation only knows which instance it is.
 */
@ExtendWith(MockitoExtension.class)
class TwoTierRecommendationCacheAdapterTest {

    @Mock
    private TwoTierCache<String, List<RecommendationItemResult>> homeFeedCache;
    @Mock
    private TwoTierCache<String, List<RecommendationItemResult>> similarProductsCache;
    @Mock
    private TwoTierCache<String, List<RecommendationItemResult>> trendingCache;
    @Mock
    private RecommendationMetricsPort metrics;

    private TwoTierRecommendationCacheAdapter adapter() {
        return new TwoTierRecommendationCacheAdapter(
                homeFeedCache, similarProductsCache, trendingCache, metrics);
    }

    /** Every cache starts cold, so a read-through reaches the loader unless a test says otherwise. */
    private void allCachesMiss() {
        lenient().when(homeFeedCache.get(any())).thenReturn(Optional.empty());
        lenient().when(similarProductsCache.get(any())).thenReturn(Optional.empty());
        lenient().when(trendingCache.get(any())).thenReturn(Optional.empty());
    }

    @Test
    void theHomeSurfaceUsesTheProfileCadenceCache() {
        // Profiles and popularity are rebuilt every fifteen minutes; a longer TTL here would hide a
        // refreshed profile from the user who caused it.
        allCachesMiss();

        adapter().getOrLoad(RecommendationSurface.HOME, "user-1", List::of);

        verify(homeFeedCache).get("user-1");
        verify(homeFeedCache).put("user-1", List.of());
        verify(similarProductsCache, never()).get(any());
        verify(trendingCache, never()).get(any());
    }

    @Test
    void everyProductScopedSurfaceUsesTheSimilarityCadenceCache() {
        // Similarity is rebuilt hourly, so these can hold entries far longer than the home feed.
        allCachesMiss();
        TwoTierRecommendationCacheAdapter adapter = adapter();

        adapter.getOrLoad(RecommendationSurface.SIMILAR, "p-1", List::of);
        adapter.getOrLoad(RecommendationSurface.ALSO_BOUGHT, "also-bought:p-1", List::of);
        adapter.getOrLoad(RecommendationSurface.CART, "cart:p-1", List::of);

        verify(similarProductsCache).get("p-1");
        verify(similarProductsCache).get("also-bought:p-1");
        verify(similarProductsCache).get("cart:p-1");
        verify(homeFeedCache, never()).get(any());
    }

    @Test
    void everySurfaceRoutesSomewhere() {
        // A new surface added to the enum without a branch here would throw at runtime on that surface
        // only, which is exactly the kind of gap that reaches production.
        allCachesMiss();

        for (RecommendationSurface surface : RecommendationSurface.values()) {
            assertThat(adapter().getOrLoad(surface, "key", List::of)).isNotNull();
        }
    }

    @Test
    void trendingSharesOneEntryForEveryone() {
        // The slate has no per-user component, so keying it by caller would multiply identical entries
        // and drop the hit rate for no benefit.
        allCachesMiss();

        adapter().getOrLoadTrending(List::of);

        verify(trendingCache).get("trending");
        verify(homeFeedCache, never()).get(any());
    }

    @Test
    void trendingIsTaggedAsTheHomeSurfaceItServes() {
        // There is no TRENDING surface: the slate is what an anonymous visitor sees on the home feed, and
        // splitting the tag would separate the two halves of one graph.
        allCachesMiss();

        adapter().getOrLoadTrending(List::of);

        verify(metrics).recordCacheMiss(RecommendationSurface.HOME);
    }

    @Test
    void aHitIsCountedAgainstTheSurfaceRatherThanTheCacheInstance() {
        // Similar and also-bought share one cache but are separate products with separate hit rates,
        // which is the dimension worth graphing.
        List<RecommendationItemResult> cached = List.of();
        when(similarProductsCache.get("p-1")).thenReturn(Optional.of(cached));

        assertThat(adapter().getOrLoad(RecommendationSurface.SIMILAR, "p-1", List::of))
                .isSameAs(cached);

        verify(metrics).recordCacheHit(RecommendationSurface.SIMILAR);
        verify(metrics, never()).recordCacheMiss(any());
        verify(similarProductsCache, never()).put(any(), any());
    }

    @Test
    void aMissIsCountedAndTheLoadedSlateIsStored() {
        when(similarProductsCache.get("also-bought:p-1")).thenReturn(Optional.empty());
        List<RecommendationItemResult> loaded = List.of();

        assertThat(adapter().getOrLoad(RecommendationSurface.ALSO_BOUGHT, "also-bought:p-1", () -> loaded))
                .isSameAs(loaded);

        verify(metrics).recordCacheMiss(RecommendationSurface.ALSO_BOUGHT);
        verify(similarProductsCache).put("also-bought:p-1", loaded);
    }

    @Test
    void aLoaderThatYieldsNothingIsNotStored() {
        // Caching an absent value would serve a miss as a hit for the whole TTL. The caller falls back to
        // trending on an empty slate, so the next request should get another chance to compute one.
        when(homeFeedCache.get("user-1")).thenReturn(Optional.empty());

        assertThat(adapter().getOrLoad(RecommendationSurface.HOME, "user-1", () -> null)).isNull();

        verify(metrics).recordCacheMiss(RecommendationSurface.HOME);
        verify(homeFeedCache, never()).put(any(), any());
    }

    @Test
    void evictionAddressesTheHomeFeedByUserAlone() {
        // Which is only possible because the key is the bare user ID rather than a (user, limit) pair.
        adapter().evictUserFeed("user-1");

        verify(homeFeedCache).evict("user-1");
        verify(similarProductsCache, never()).evict(any());
        verify(trendingCache, never()).evict(any());
    }
}
