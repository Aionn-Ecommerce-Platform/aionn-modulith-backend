package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.policy.ColdStartPolicy;
import com.aionn.recommendation.application.policy.HybridRankingPolicy;
import com.aionn.recommendation.application.policy.RankingWeightPolicy;
import com.aionn.recommendation.application.port.out.ProductAttributeQueryPort;
import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.domain.model.RecommendationSlate;
import com.aionn.recommendation.domain.model.UserAffinityProfile;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import com.aionn.sharedkernel.integration.port.promotion.FlashSaleQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produces a ranked, over-fetched slate per surface; availability filtering happens downstream.
 * {@code NOT_SUPPORTED} because each collaborator opens its own short read-only transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RecommendationService {

    private static final Duration SEED_LOOKBACK = Duration.ofDays(30);
    private static final int SEED_LIMIT = 20;

    /** Largest page any surface will build. Mirrors the {@code @Max} on the controller's limit. */
    private static final int MAX_PAGE_SIZE = 50;

    /** Cap the share of a cold-start slate that may be flash-sale products. */
    private static final double MAX_FLASH_SALE_SHARE = 0.40;

    private final CandidateGenerationService candidateGeneration;
    private final ProfileRefreshService profileRefreshService;
    private final HybridRankingPolicy rankingPolicy;
    private final ColdStartPolicy coldStartPolicy;
    private final RankingWeightPolicy weights;
    private final RecommendationMetricsPort metrics;
    private final FlashSaleQueryPort flashSaleQuery;

    public List<RecommendationItemResult> homeFeed(String userId, int limit) {
        int candidateLimit = candidateLimit(limit);
        UserAffinityProfile profile = profileRefreshService.profileOf(userId);
        ColdStartPolicy.SignalWeights signalWeights = coldStartPolicy.resolve(profile);
        if (signalWeights.isPopularityOnly()) {
            return fallbackToTrending(RecommendationSurface.HOME, limit, Set.of());
        }

        Map<String, BigDecimal> collaborative = signalWeights.usesCollaborative()
                ? candidateGeneration.collaborativeScores(
                        candidateGeneration.recentSeedProductIds(userId, SEED_LOOKBACK, SEED_LIMIT),
                        candidateLimit)
                : Map.of();
        Map<String, BigDecimal> content = signalWeights.usesContent()
                ? candidateGeneration.contentScores(profile, candidateLimit)
                : Map.of();
        Map<String, BigDecimal> popularity = candidateGeneration.popularityScores(candidateLimit);

        // Exclude owned products only; viewed/carted items are valid to resurface.
        Set<String> excluded = new HashSet<>(candidateGeneration.purchasedProductIds(userId));

        List<RecommendationItemResult> results = rankAndHydrate(
                RecommendationSurface.HOME,
                new HybridRankingPolicy.SignalScores(collaborative, content, popularity),
                signalWeights,
                excluded,
                candidateLimit);
        return results.isEmpty()
                ? fallbackToTrending(RecommendationSurface.HOME, limit, excluded)
                : results;
    }

    public List<RecommendationItemResult> similarProducts(String productId, int limit) {
        return productSurface(RecommendationSurface.SIMILAR, productId, limit);
    }

    public List<RecommendationItemResult> alsoBought(String productId, int limit) {
        return productSurface(RecommendationSurface.ALSO_BOUGHT, productId, limit);
    }

    public List<RecommendationItemResult> cartSuggestions(
            String userId, List<String> cartSkuIds, int limit) {
        int candidateLimit = candidateLimit(limit);
        List<String> seedProductIds = candidateGeneration.resolveProductIds(cartSkuIds);
        if (seedProductIds.isEmpty()) {
            return homeFeed(userId, limit);
        }

        Map<String, BigDecimal> collaborative = candidateGeneration.collaborativeScores(seedProductIds, candidateLimit);
        Map<String, BigDecimal> popularity = candidateGeneration.popularityScores(candidateLimit);

        Set<String> excluded = new HashSet<>(seedProductIds);
        if (isRealUser(userId)) {
            excluded.addAll(candidateGeneration.purchasedProductIds(userId));
        }

        List<RecommendationItemResult> results = rankAndHydrate(
                RecommendationSurface.CART,
                new HybridRankingPolicy.SignalScores(collaborative, Map.of(), popularity),
                coldStartPolicy.forProductSurface(),
                excluded,
                candidateLimit);
        return results.isEmpty()
                ? fallbackToTrending(RecommendationSurface.CART, limit, excluded)
                : results;
    }

    /** Cold start: recent momentum, topped up with new arrivals. */
    public List<RecommendationItemResult> trending(int limit) {
        return trending(limit, Set.of());
    }

    public List<RecommendationItemResult> trending(int limit, Set<String> excluded) {
        int candidateLimit = candidateLimit(limit);
        Map<String, BigDecimal> popularity = candidateGeneration.popularityScores(candidateLimit);
        Map<String, BigDecimal> newArrivals = popularity.size() >= candidateLimit
                ? Map.of()
                : candidateGeneration.newArrivalScores(candidateLimit);

        return rankAndHydrate(
                RecommendationSurface.HOME,
                new HybridRankingPolicy.SignalScores(Map.of(), newArrivals, popularity),
                ColdStartPolicy.SignalWeights.trendingWithNewArrivals(),
                excluded == null ? Set.of() : excluded,
                candidateLimit);
    }

    /**
     * Applies the flash-sale cap to the ranked result so every candidate source respects it.
     * Over-cap sale items are dropped, not re-scored.
     */
    private List<RecommendationItemResult> capFlashSaleShare(
            List<RecommendationItemResult> results, int candidateLimit) {
        if (results == null || results.isEmpty() || flashSaleQuery == null) {
            return results;
        }
        Map<String, FlashSaleQueryPort.ProductFlashSale> onSale = flashSaleQuery
                .findActiveByProductIds(results.stream().map(RecommendationItemResult::productId).toList());
        if (onSale.isEmpty()) {
            return results;
        }
        int saleBudget = Math.max(1, (int) Math.floor(results.size() * MAX_FLASH_SALE_SHARE));
        List<RecommendationItemResult> nonSale = new ArrayList<>(results.size());
        List<RecommendationItemResult> sale = new ArrayList<>();
        for (RecommendationItemResult item : results) {
            if (onSale.containsKey(item.productId())) {
                sale.add(item);
            } else {
                nonSale.add(item);
            }
        }
        // Non-sale first so the visible head can't be all flash-sale items.
        nonSale.addAll(sale.subList(0, Math.min(saleBudget, sale.size())));
        return nonSale;
    }

    private List<RecommendationItemResult> productSurface(
            RecommendationSurface surface, String productId, int limit) {
        int candidateLimit = candidateLimit(limit);
        if (!candidateGeneration.productExists(productId)) {
            return fallbackToTrending(surface, limit, Set.of(productId));
        }

        Map<String, BigDecimal> collaborative = candidateGeneration.collaborativeScores(List.of(productId),
                candidateLimit);
        if (collaborative == null) {
            collaborative = Map.of();
        }

        Map<String, BigDecimal> contentCandidates = Map.of();
        Map<String, BigDecimal> popularity;

        if (collaborative.isEmpty()) {
            Map<String, BigDecimal> categoryCandidates = candidateGeneration.categoryCandidates(productId, candidateLimit);
            if (categoryCandidates != null && !categoryCandidates.isEmpty()) {
                contentCandidates = categoryCandidates;
                popularity = candidateGeneration.popularityScoresFor(contentCandidates.keySet());
            } else {
                popularity = candidateGeneration.popularityScores(candidateLimit);
            }
        } else {
            popularity = candidateGeneration.popularityScoresFor(collaborative.keySet());
        }

        if (popularity == null) {
            popularity = Map.of();
        }

        ColdStartPolicy.SignalWeights signalWeights = contentCandidates.isEmpty()
                ? coldStartPolicy.forProductSurface()
                : coldStartPolicy.forProductSurfaceWithContent();

        List<RecommendationItemResult> results = rankAndHydrate(
                surface,
                new HybridRankingPolicy.SignalScores(collaborative, contentCandidates, popularity),
                signalWeights,
                Set.of(productId),
                candidateLimit);
        return results.isEmpty() ? fallbackToTrending(surface, limit, Set.of(productId)) : results;
    }

    /** Falls back to trending and records the fallback so cold starts are distinguishable from breakage. */
    private List<RecommendationItemResult> fallbackToTrending(
            RecommendationSurface surface, int limit, Set<String> excluded) {
        metrics.recordTrendingFallback(surface);
        return trending(limit, excluded);
    }

    /** Drops products catalog has retired, since the interaction log keeps stale IDs. */
    private List<RecommendationItemResult> rankAndHydrate(
            RecommendationSurface surface,
            HybridRankingPolicy.SignalScores signals,
            ColdStartPolicy.SignalWeights signalWeights,
            Set<String> excluded,
            int candidateLimit) {

        RecommendationSlate slate = rankingPolicy.rank(surface, signals, signalWeights, excluded, candidateLimit);
        if (slate.isEmpty()) {
            return List.of();
        }

        Map<String, ProductAttributeQueryPort.ProductAttributes> attributes = candidateGeneration
                .hydrate(slate.productIds());
        if (attributes.isEmpty()) {
            return List.of();
        }

        List<RecommendationItemResult> results = new ArrayList<>(slate.items().size());
        for (RecommendationSlate.ScoredProduct scored : slate.items()) {
            ProductAttributeQueryPort.ProductAttributes product = attributes.get(scored.productId());
            if (product == null) {
                continue;
            }
            results.add(new RecommendationItemResult(
                    product.productId(),
                    product.name(),
                    product.imageUrl(),
                    product.priceFrom(),
                    product.originalPriceFrom(),
                    product.currency(),
                    scored.score().value(),
                    scored.reason(),
                    product.skuIds()));
        }
        return capFlashSaleShare(results, candidateLimit);
    }

    /** Over-fetch so downstream availability filtering still leaves a full page; clamp to avoid int overflow. */
    private int candidateLimit(int limit) {
        int page = Math.clamp(limit, 1, MAX_PAGE_SIZE);
        return (int) Math.min((long) page * weights.candidateOverFetchFactor(), weights.maxCandidates());
    }

    private static boolean isRealUser(String userId) {
        return userId != null && !userId.isBlank() && !"anonymousUser".equals(userId);
    }
}
