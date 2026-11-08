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
 * Read-side entry point: serve the cached ranked slate, then filter by availability and truncate.
 * Ordering matters: ranking is cached, availability is applied per request. {@code NOT_SUPPORTED}
 * keeps the availability call outside any transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RecommendationReadService {

    /** Compute slates at the largest allowed size so one cache entry serves every page size. */
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

    /** Not cached: the SKU-set key rarely repeats, so hit rate would be near zero. */
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

    /** Times the whole request path, including the per-request availability filter. */
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

    /** Drops products with no available SKU, then truncates; clamps limit to avoid a negative capacity. */
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

    /** Keep products with no SKUs (catalog gap); otherwise require at least one in stock. */
    private static boolean hasAvailableSku(RecommendationItemResult item, Set<String> availableSkus) {
        return item.skuIds().isEmpty() || item.skuIds().stream().anyMatch(availableSkus::contains);
    }

    private static boolean isRealUser(String userId) {
        return userId != null && !userId.isBlank() && !"anonymousUser".equals(userId);
    }
}
