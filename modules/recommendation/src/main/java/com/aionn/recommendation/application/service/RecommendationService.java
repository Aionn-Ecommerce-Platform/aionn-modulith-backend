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
 * Produces a ranked, hydrated slate for each surface.
 *
 * <p>
 * Returns the full over-fetched list rather than the requested page:
 * availability filtering happens
 * downstream in {@link RecommendationReadService}, and filtering a list of
 * exactly N would leave fewer
 * than N.
 *
 * <p>
 * {@code NOT_SUPPORTED} because each collaborator below opens its own short
 * read-only transaction.
 * Holding one across the whole request would keep a connection busy for the
 * duration, and would stay
 * open across the catalog call - which becomes a network hop once this module
 * is split out.
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

    private final CandidateGenerationService candidateGeneration;
    private final ProfileRefreshService profileRefreshService;
    private final HybridRankingPolicy rankingPolicy;
    private final ColdStartPolicy coldStartPolicy;
    private final RankingWeightPolicy weights;
    private final RecommendationMetricsPort metrics;

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

        // Owning something is a reason not to be sold it again. Views and cart adds are
        // not excluded:
        // resurfacing a product the user browsed is exactly what the feed is for.
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

        // Nothing already in the basket, and nothing the user has bought before.
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

    /**
     * Cold-start answer: recent momentum, topped up with new arrivals when momentum
     * data is thin -
     * which is the normal state of a freshly seeded catalog.
     */
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

    private List<RecommendationItemResult> productSurface(
            RecommendationSurface surface, String productId, int limit) {
        int candidateLimit = candidateLimit(limit);
        if (!candidateGeneration.productExists(productId)) {
            return fallbackToTrending(surface, limit, Set.of(productId));
        }

        Map<String, BigDecimal> collaborative = candidateGeneration.collaborativeScores(List.of(productId),
                candidateLimit);
        // With no co-occurrence data yet, fall back to the global popularity pool;
        // otherwise score only
        // the neighbours so popularity breaks ties rather than introducing unrelated
        // products.
        Map<String, BigDecimal> popularity = collaborative.isEmpty()
                ? candidateGeneration.popularityScores(candidateLimit)
                : candidateGeneration.popularityScoresFor(collaborative.keySet());

        List<RecommendationItemResult> results = rankAndHydrate(
                surface,
                new HybridRankingPolicy.SignalScores(collaborative, Map.of(), popularity),
                coldStartPolicy.forProductSurface(),
                Set.of(productId),
                candidateLimit);
        return results.isEmpty() ? fallbackToTrending(surface, limit, Set.of(productId)) : results;
    }

    /**
     * Serves trending in place of a surface that produced nothing, and says so.
     *
     * <p>The counter is the only visible difference between "this catalogue has no co-purchase data yet"
     * and "personalisation quietly stopped working": both render a perfectly reasonable-looking slate of
     * popular products, so neither the caller nor the log can tell them apart. Counting cold starts too
     * is deliberate - a profile pipeline that broke shows up here as a sustained spike, which is exactly
     * when it needs to show up.
     */
    private List<RecommendationItemResult> fallbackToTrending(
            RecommendationSurface surface, int limit, Set<String> excluded) {
        metrics.recordTrendingFallback(surface);
        return trending(limit, excluded);
    }

    /**
     * Ranks, then attaches the display fields. Products catalog no longer exposes -
     * unpublished, taken
     * down, deleted - drop out here, because the interaction log keeps IDs that
     * catalog may have retired.
     */
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
                    product.currency(),
                    scored.score().value(),
                    scored.reason(),
                    product.skuIds()));
        }
        return results;
    }

    /**
     * Over-fetch so availability filtering downstream still leaves a full page.
     *
     * <p>The page size is clamped and the product taken in {@code long} because this is reachable from a
     * public endpoint. An unclamped {@code limit * factor} overflows {@code int} for a large enough page
     * size and comes back negative, and a negative capacity then fails the {@code ArrayList} the caller
     * builds with it - a 500 on an anonymous request rather than a rejected parameter. Clamping here
     * rather than relying on the controller keeps the invariant inside the service that depends on it.
     * Must stay in step with the {@code @Max} on the controller's limit parameter.
     */
    private int candidateLimit(int limit) {
        int page = Math.clamp(limit, 1, MAX_PAGE_SIZE);
        return (int) Math.min((long) page * weights.candidateOverFetchFactor(), weights.maxCandidates());
    }

    private static boolean isRealUser(String userId) {
        return userId != null && !userId.isBlank() && !"anonymousUser".equals(userId);
    }
}
