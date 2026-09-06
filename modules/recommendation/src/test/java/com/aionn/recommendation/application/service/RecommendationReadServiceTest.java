package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.out.RecommendationCachePort;
import com.aionn.recommendation.application.port.out.StockAvailabilityQueryPort;
import com.aionn.recommendation.domain.valueobject.RecommendationReason;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationReadServiceTest {

    @Mock private RecommendationService recommendationService;
    @Mock private RecommendationCachePort cache;
    @Mock private StockAvailabilityQueryPort stockAvailability;

    private RecommendationReadService service() {
        return new RecommendationReadService(recommendationService, cache, stockAvailability);
    }

    @Test
    void outOfStockProductsAreDroppedAfterRanking() {
        // Availability is applied outside the cache precisely so this can differ between two requests
        // that share a cached slate.
        cacheReturnsSlate(RecommendationSurface.HOME, List.of(
                item("p-available", "sku-1"), item("p-sold-out", "sku-2")));
        when(stockAvailability.filterAvailableSkus(any())).thenReturn(Set.of("sku-1"));

        List<RecommendationItemResult> results = service().homeFeed("user-1", 10);

        assertThat(results).extracting(RecommendationItemResult::productId)
                .containsExactly("p-available");
    }

    @Test
    void theOverFetchedSlateIsTruncatedToTheRequestedSize() {
        cacheReturnsSlate(RecommendationSurface.HOME, List.of(
                item("p-1", "sku-1"), item("p-2", "sku-2"), item("p-3", "sku-3")));
        when(stockAvailability.filterAvailableSkus(any()))
                .thenReturn(Set.of("sku-1", "sku-2", "sku-3"));

        assertThat(service().homeFeed("user-1", 2)).hasSize(2);
    }

    @Test
    void aProductWithoutSkusIsKeptRatherThanSilentlyDropped() {
        // No SKUs is a catalog modelling gap, not an availability answer; hiding it would shrink every
        // slate for reasons the user cannot see.
        cacheReturnsSlate(RecommendationSurface.HOME, List.of(item("p-no-skus")));
        when(stockAvailability.filterAvailableSkus(any())).thenReturn(Set.of());

        assertThat(service().homeFeed("user-1", 10))
                .extracting(RecommendationItemResult::productId)
                .containsExactly("p-no-skus");
    }

    @Test
    void anAnonymousCallerGetsTrendingWithoutTouchingTheUserFeedCache() {
        when(cache.getOrLoadTrending(any())).thenAnswer(invocation -> {
            Supplier<List<RecommendationItemResult>> loader = invocation.getArgument(0);
            return loader.get();
        });
        when(recommendationService.trending(anyInt())).thenReturn(List.of(item("p-trending", "sku-1")));
        when(stockAvailability.filterAvailableSkus(any())).thenReturn(Set.of("sku-1"));

        List<RecommendationItemResult> results = service().homeFeed(null, 10);

        assertThat(results).extracting(RecommendationItemResult::productId)
                .containsExactly("p-trending");
        verify(cache, never()).getOrLoad(eq(RecommendationSurface.HOME), anyString(), any());
    }

    @Test
    void anonymousUserPrincipalIsTreatedAsUnauthenticated() {
        when(cache.getOrLoadTrending(any())).thenReturn(List.of());

        service().homeFeed("anonymousUser", 10);

        verify(cache, never()).getOrLoad(eq(RecommendationSurface.HOME), anyString(), any());
    }

    @Test
    void theHomeFeedIsCachedByUserSoTheKeyDoesNotVaryWithPageSize() {
        // One entry per user rather than per (user, limit) pair: eviction can address it by user alone,
        // and a client changing its page size still hits the cache.
        cacheReturnsSlate(RecommendationSurface.HOME, List.of(item("p-1", "sku-1")));
        when(stockAvailability.filterAvailableSkus(any())).thenReturn(Set.of("sku-1"));

        service().homeFeed("user-1", 5);

        verify(cache).getOrLoad(eq(RecommendationSurface.HOME), eq("user-1"), any());
    }

    @Test
    void cartSuggestionsBypassTheCacheBecauseTheBasketIsTheKey() {
        // A cache keyed on an arbitrary SKU set would almost never hit, costing memory for nothing.
        when(recommendationService.cartSuggestions("user-1", List.of("sku-9"), 5))
                .thenReturn(List.of(item("p-1", "sku-1")));
        when(stockAvailability.filterAvailableSkus(any())).thenReturn(Set.of("sku-1"));

        service().cartSuggestions("user-1", List.of("sku-9"), 5);

        verify(cache, never()).getOrLoad(any(), anyString(), any());
    }

    @Test
    void anEmptySlateSkipsTheAvailabilityCallEntirely() {
        cacheReturnsSlate(RecommendationSurface.HOME, List.of());

        assertThat(service().homeFeed("user-1", 10)).isEmpty();
        verify(stockAvailability, never()).filterAvailableSkus(any());
    }

    @Test
    void aNullSlateFromTheCacheIsTreatedAsEmpty() {
        // The port's contract permits it, and an NPE here would take down every surface at once.
        cacheReturnsSlate(RecommendationSurface.HOME, null);

        assertThat(service().homeFeed("user-1", 10)).isEmpty();
    }

    @Test
    void eachProductSurfaceIsCachedUnderItsOwnKey() {
        // Similar and also-bought answer different questions about the same product, so sharing a key
        // would serve one surface's slate on the other.
        cacheReturnsSlate(RecommendationSurface.SIMILAR, List.of(item("p-1", "sku-1")));
        cacheReturnsSlate(RecommendationSurface.ALSO_BOUGHT, List.of(item("p-2", "sku-2")));
        when(stockAvailability.filterAvailableSkus(any())).thenReturn(Set.of("sku-1", "sku-2"));

        service().similarProducts("p-seed", 10);
        service().alsoBought("p-seed", 10);

        verify(cache).getOrLoad(eq(RecommendationSurface.SIMILAR), eq("p-seed"), any());
        verify(cache).getOrLoad(eq(RecommendationSurface.ALSO_BOUGHT), eq("also-bought:p-seed"), any());
    }

    @Test
    void everySurfaceComputesTheSameSlateSizeWhateverThePageSizeAsked() {
        // One entry per subject rather than per (subject, limit) pair. The loader is what would vary, so
        // it is the loader that is checked here.
        when(cache.getOrLoad(any(), anyString(), any())).thenAnswer(invocation -> {
            Supplier<List<RecommendationItemResult>> loader = invocation.getArgument(2);
            return loader.get();
        });
        when(cache.getOrLoadTrending(any())).thenAnswer(invocation -> {
            Supplier<List<RecommendationItemResult>> loader = invocation.getArgument(0);
            return loader.get();
        });

        RecommendationReadService service = service();
        service.homeFeed("user-1", 3);
        service.similarProducts("p-1", 3);
        service.alsoBought("p-1", 3);
        service.trending(3);

        verify(recommendationService).homeFeed("user-1", 50);
        verify(recommendationService).similarProducts("p-1", 50);
        verify(recommendationService).alsoBought("p-1", 50);
        verify(recommendationService).trending(50);
    }

    @Test
    void cartSuggestionsAreComputedAtTheRequestedSizeBecauseTheyAreNotCached() {
        // Nothing is reused, so over-fetching would only cost work - the ranking runs per request either
        // way.
        when(recommendationService.cartSuggestions("user-1", List.of("sku-9"), 3))
                .thenReturn(List.of(item("p-1", "sku-1")));
        when(stockAvailability.filterAvailableSkus(any())).thenReturn(Set.of("sku-1"));

        assertThat(service().cartSuggestions("user-1", List.of("sku-9"), 3)).hasSize(1);
    }

    @Test
    void everySkuInTheSlateIsCheckedOnceRegardlessOfHowManyProductsShareIt() {
        // One availability call per request, not per product: this is the only external hop on the read
        // path and it is about to become a network call.
        cacheReturnsSlate(RecommendationSurface.HOME, List.of(
                item("p-1", "sku-1", "sku-2"), item("p-2", "sku-2", "sku-3")));
        when(stockAvailability.filterAvailableSkus(any())).thenReturn(Set.of("sku-1"));

        service().homeFeed("user-1", 10);

        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.captor();
        verify(stockAvailability).filterAvailableSkus(captor.capture());
        assertThat(captor.getValue()).containsExactly("sku-1", "sku-2", "sku-3");
    }

    @Test
    void aBlankUserIdIsTreatedAsUnauthenticated() {
        // Spring hands over an empty principal name in some filter-chain configurations; an empty-string
        // cache key would collect every anonymous caller into one shared "user" entry.
        when(cache.getOrLoadTrending(any())).thenReturn(List.of());

        service().homeFeed("   ", 10);

        verify(cache, never()).getOrLoad(eq(RecommendationSurface.HOME), anyString(), any());
    }

    private void cacheReturnsSlate(
            RecommendationSurface surface, List<RecommendationItemResult> slate) {
        when(cache.getOrLoad(eq(surface), anyString(), any())).thenReturn(slate);
    }

    private static RecommendationItemResult item(String productId, String... skuIds) {
        return new RecommendationItemResult(
                productId,
                "Product " + productId,
                null,
                BigDecimal.valueOf(1000),
                "VND",
                BigDecimal.valueOf(0.5),
                RecommendationReason.TRENDING,
                List.of(skuIds));
    }
}
