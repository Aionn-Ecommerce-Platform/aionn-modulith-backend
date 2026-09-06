package com.aionn.recommendation.infrastructure.cache;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The routing is the whole adapter, and getting it wrong is silent: a slate
 * would still be served, just
 * with the wrong TTL behind it.
 */
@ExtendWith(MockitoExtension.class)
class TwoTierRecommendationCacheAdapterTest {

    @Mock
    private TwoTierCache<String, List<RecommendationItemResult>> homeFeedCache;
    @Mock
    private TwoTierCache<String, List<RecommendationItemResult>> similarProductsCache;
    @Mock
    private TwoTierCache<String, List<RecommendationItemResult>> trendingCache;

    private TwoTierRecommendationCacheAdapter adapter() {
        return new TwoTierRecommendationCacheAdapter(
                homeFeedCache, similarProductsCache, trendingCache);
    }

    @Test
    void theHomeSurfaceUsesTheProfileCadenceCache() {
        // Profiles and popularity are rebuilt every fifteen minutes; a longer TTL here
        // would hide a
        // refreshed profile from the user who caused it.
        adapter().getOrLoad(RecommendationSurface.HOME, "user-1", List::of);

        verify(homeFeedCache).getOrLoad(eq("user-1"), any());
        verify(similarProductsCache, never()).getOrLoad(any(), any());
    }

    @Test
    void everyProductScopedSurfaceUsesTheSimilarityCadenceCache() {
        // Similarity is rebuilt hourly, so these three can hold entries far longer than
        // the home feed.
        TwoTierRecommendationCacheAdapter adapter = adapter();

        adapter.getOrLoad(RecommendationSurface.SIMILAR, "p-1", List::of);
        adapter.getOrLoad(RecommendationSurface.ALSO_BOUGHT, "also-bought:p-1", List::of);
        adapter.getOrLoad(RecommendationSurface.CART, "cart:p-1", List::of);

        verify(similarProductsCache).getOrLoad(eq("p-1"), any());
        verify(similarProductsCache).getOrLoad(eq("also-bought:p-1"), any());
        verify(similarProductsCache).getOrLoad(eq("cart:p-1"), any());
        verify(homeFeedCache, never()).getOrLoad(any(), any());
    }

    @Test
    void everySurfaceRoutesSomewhere() {
        // A new surface added to the enum without a branch here would throw at runtime
        // on that surface
        // only, which is exactly the kind of gap that reaches production.
        when(homeFeedCache.getOrLoad(any(), any())).thenReturn(List.of());
        when(similarProductsCache.getOrLoad(any(), any())).thenReturn(List.of());

        for (RecommendationSurface surface : RecommendationSurface.values()) {
            assertThat(adapter().getOrLoad(surface, "key", List::of)).isNotNull();
        }
    }

    @Test
    void trendingSharesOneEntryForEveryone() {
        // The slate has no per-user component, so keying it by caller would multiply
        // identical entries
        // and drop the hit rate for no benefit.
        adapter().getOrLoadTrending(List::of);

        verify(trendingCache).getOrLoad(eq("trending"), any());
    }

    @Test
    void theLoaderIsPassedThroughRatherThanInvokedEagerly() {
        // Calling it here would compute a slate on every request, cache hit or not.
        when(trendingCache.getOrLoad(eq("trending"), any())).thenAnswer(invocation -> {
            Supplier<List<RecommendationItemResult>> loader = invocation.getArgument(1);
            return loader.get();
        });

        assertThat(adapter().getOrLoadTrending(List::of)).isEmpty();
    }

    @Test
    void evictionAddressesTheHomeFeedByUserAlone() {
        // Which is only possible because the key is the bare user ID rather than a
        // (user, limit) pair.
        adapter().evictUserFeed("user-1");

        verify(homeFeedCache).evict("user-1");
        verify(similarProductsCache, never()).evict(any());
        verify(trendingCache, never()).evict(any());
    }
}
