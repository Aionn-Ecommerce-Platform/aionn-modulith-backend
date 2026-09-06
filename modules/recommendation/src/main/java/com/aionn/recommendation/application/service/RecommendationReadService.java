package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.out.RecommendationCachePort;
import com.aionn.recommendation.application.port.out.StockAvailabilityQueryPort;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
 * {@code document/architecture.md} a read-only transaction is still a transaction.
 */
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

    public List<RecommendationItemResult> homeFeed(String userId, int limit) {
        if (!isRealUser(userId)) {
            return trending(limit);
        }
        List<RecommendationItemResult> slate = cache.getOrLoad(
                RecommendationSurface.HOME,
                userId,
                () -> recommendationService.homeFeed(userId, CACHEABLE_SLATE_SIZE));
        return filterAndTruncate(slate, limit);
    }

    public List<RecommendationItemResult> similarProducts(String productId, int limit) {
        List<RecommendationItemResult> slate = cache.getOrLoad(
                RecommendationSurface.SIMILAR,
                productId,
                () -> recommendationService.similarProducts(productId, CACHEABLE_SLATE_SIZE));
        return filterAndTruncate(slate, limit);
    }

    public List<RecommendationItemResult> alsoBought(String productId, int limit) {
        List<RecommendationItemResult> slate = cache.getOrLoad(
                RecommendationSurface.ALSO_BOUGHT,
                "also-bought:" + productId,
                () -> recommendationService.alsoBought(productId, CACHEABLE_SLATE_SIZE));
        return filterAndTruncate(slate, limit);
    }

    /**
     * Cart suggestions are not cached: the basket is the input, so the key would be a set of SKUs that
     * rarely repeats between users. A near-zero hit rate would cost memory without saving work.
     */
    public List<RecommendationItemResult> cartSuggestions(
            String userId, List<String> cartSkuIds, int limit) {
        return filterAndTruncate(
                recommendationService.cartSuggestions(userId, cartSkuIds, limit), limit);
    }

    public List<RecommendationItemResult> trending(int limit) {
        List<RecommendationItemResult> slate = cache.getOrLoadTrending(
                () -> recommendationService.trending(CACHEABLE_SLATE_SIZE));
        return filterAndTruncate(slate, limit);
    }

    /**
     * Drops products with no fulfillable SKU, then cuts the over-fetched slate down to the requested
     * page size.
     */
    private List<RecommendationItemResult> filterAndTruncate(
            List<RecommendationItemResult> slate, int limit) {
        if (slate == null || slate.isEmpty()) {
            return List.of();
        }
        Set<String> availableSkus = stockAvailability.filterAvailableSkus(
                slate.stream().flatMap(item -> item.skuIds().stream()).distinct().toList());

        List<RecommendationItemResult> results = new ArrayList<>(Math.min(limit, slate.size()));
        for (RecommendationItemResult item : slate) {
            if (results.size() == limit) {
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
