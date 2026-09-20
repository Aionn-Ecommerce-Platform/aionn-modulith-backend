package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.out.RecommendationCachePort;
import com.aionn.recommendation.application.port.out.StockAvailabilityQueryPort;
import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Read-side entry point for every recommendation surface: cache the ranked slate, then filter by
 * availability and truncate.
 *
 * <p>The ordering matters. Ranking is expensive and stable for minutes, so it is cached. Availability
 * is cheap and changes by the second, so it is applied on every request outside the cache. Reversing
 * that would serve out-of-stock products for the whole TTL.
 *
 * <p>{@code NOT_SUPPORTED}: the availability call must not run inside a transaction. It is an
 * in-process adapter today but becomes a network call when this module is split out, and per
 * {@code docs/architecture/modular-monolith.md} a read-only transaction is still a transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RecommendationReadService {

    /**
     * Slates are always computed at the largest size any caller may request, then truncated per
     * request. This keeps one cache entry per user or product rather than one per (subject, limit)
     * pair, which matters for two reasons: eviction can address an entry by subject alone, and a
     * client changing its page size does not miss the cache. Must stay in step with the {@code @Max}
     * on the controller's limit parameter.
     */
    private static final int CACHEABLE_SLATE_SIZE = 50;

    private final RecommendationService recommendationService;
    private final RecommendationCachePort cache;
    private final StockAvailabilityQueryPort stockAvailability;
    private final RecommendationMetricsPort metrics;

    public List<RecommendationItemResult> homeFeed(String userId, int limit) {
        if (!isRealUser(userId)) {
            return trending(limit);
        }
        return timed(RecommendationSurface.HOME, () -> filterAndTruncate(
                cache.getOrLoad(
                        RecommendationSurface.HOME,
                        userId,
                        () -> recommendationService.homeFeed(userId, CACHEABLE_SLATE_SIZE)),
                limit));
    }

    public List<RecommendationItemResult> similarProducts(String productId, int limit) {
        return timed(RecommendationSurface.SIMILAR, () -> filterAndTruncate(
                cache.getOrLoad(
                        RecommendationSurface.SIMILAR,
                        productId,
                        () -> recommendationService.similarProducts(productId, CACHEABLE_SLATE_SIZE)),
                limit));
    }

    public List<RecommendationItemResult> alsoBought(String productId, int limit) {
        return timed(RecommendationSurface.ALSO_BOUGHT, () -> filterAndTruncate(
                cache.getOrLoad(
                        RecommendationSurface.ALSO_BOUGHT,
                        "also-bought:" + productId,
                        () -> recommendationService.alsoBought(productId, CACHEABLE_SLATE_SIZE)),
                limit));
    }

    /**
     * Cart suggestions are not cached: the basket is the input, so the key would be a set of SKUs that
     * rarely repeats between users. A near-zero hit rate would cost memory without saving work.
     */
    public List<RecommendationItemResult> cartSuggestions(
            String userId, List<String> cartSkuIds, int limit) {
        return timed(RecommendationSurface.CART, () -> filterAndTruncate(
                recommendationService.cartSuggestions(userId, cartSkuIds, limit), limit));
    }

    public List<RecommendationItemResult> trending(int limit) {
        return timed(RecommendationSurface.HOME, () -> filterAndTruncate(
                cache.getOrLoadTrending(
                        () -> recommendationService.trending(CACHEABLE_SLATE_SIZE)),
                limit));
    }

    /**
     * Times the whole request path for one surface, not just the ranking.
     *
     * <p>The availability filter runs on every request by design, so it is part of what a caller waits
     * for and part of what regresses when the inventory lookup gets slower. Measuring the ranking alone
     * would hide that behind a cache hit rate that looks fine.
     */
    private List<RecommendationItemResult> timed(
            RecommendationSurface surface, Supplier<List<RecommendationItemResult>> work) {
        long startedAt = System.nanoTime();
        try {
            return work.get();
        } finally {
            recordLatencySafely(surface, startedAt);
        }
    }

    private void recordLatencySafely(RecommendationSurface surface, long startedAt) {
        try {
            metrics.recordLatency(
                    surface, Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
        } catch (RuntimeException exception) {
            // Metrics must not replace a successful result or the original failure.
            log.warn("Could not record recommendation latency for {}", surface, exception);
        }
    }

    /**
     * Drops products with no fulfillable SKU, then cuts the over-fetched slate down to the requested
     * page size.
     *
     * <p>The page size is clamped against the slate before it is used as a capacity. This is reachable
     * from a public endpoint, and a negative limit would otherwise be handed straight to
     * {@code new ArrayList<>(...)} - a 500 on an anonymous request instead of an empty page.
     */
    private List<RecommendationItemResult> filterAndTruncate(
            List<RecommendationItemResult> slate, int limit) {
        if (slate == null || slate.isEmpty()) {
            return List.of();
        }
        int page = Math.clamp(limit, 0, slate.size());
        if (page == 0) {
            return List.of();
        }
        Set<String> availableSkus = stockAvailability.filterAvailableSkus(
                slate.stream().flatMap(item -> item.skuIds().stream()).distinct().toList());

        List<RecommendationItemResult> results = new ArrayList<>(page);
        for (RecommendationItemResult item : slate) {
            if (results.size() == page) {
                break;
            }
            if (hasAvailableSku(item, availableSkus)) {
                results.add(item);
            }
        }
        return results;
    }

    /**
     * A product with no SKUs at all is kept: that is a catalog modelling gap, and hiding it would
     * silently shrink every slate. A product that does have SKUs needs at least one in stock.
     */
    private static boolean hasAvailableSku(RecommendationItemResult item, Set<String> availableSkus) {
        return item.skuIds().isEmpty() || item.skuIds().stream().anyMatch(availableSkus::contains);
    }

    private static boolean isRealUser(String userId) {
        return userId != null && !userId.isBlank() && !"anonymousUser".equals(userId);
    }
}
