package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.policy.RankingWeightPolicy;
import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.ItemSimilarityPersistencePort;
import com.aionn.recommendation.application.port.out.PopularityPersistencePort;
import com.aionn.recommendation.application.port.out.ProductAttributeQueryPort;
import com.aionn.recommendation.domain.model.ItemSimilarity;
import com.aionn.recommendation.domain.model.ProductPopularity;
import com.aionn.recommendation.domain.model.UserAffinityProfile;
import com.aionn.recommendation.domain.model.UserInteraction;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the three raw signal maps that hybrid ranking consumes.
 *
 * <p>
 * Split out from {@link RecommendationService} so the orchestrator can stay
 * non-transactional while
 * every database read here runs in its own short read-only transaction.
 */
@Service
@RequiredArgsConstructor
public class CandidateGenerationService {

    private final InteractionPersistencePort interactionRepository;
    private final ItemSimilarityPersistencePort similarityRepository;
    private final PopularityPersistencePort popularityRepository;
    private final ProductAttributeQueryPort productAttributeQuery;
    private final RankingWeightPolicy weights;
    private final Clock clock;

    /**
     * Products the user recently engaged with; these seed the collaborative lookup.
     */
    @Transactional(readOnly = true)
    public List<String> recentSeedProductIds(String userId, Duration lookback, int limit) {
        return interactionRepository.findByUser(userId, clock.instant().minus(lookback), limit)
                .stream()
                .map(UserInteraction::getProductId)
                .distinct()
                .toList();
    }

    /**
     * Already-purchased products, excluded from slates so the feed does not re-sell
     * what they own.
     */
    @Transactional(readOnly = true)
    public List<String> purchasedProductIds(String userId) {
        return interactionRepository.findPurchasedProductIds(userId);
    }

    /**
     * Maps cart SKUs to their owning products, preserving order and dropping
     * unresolvable ones.
     */
    @Transactional(readOnly = true)
    public List<String> resolveProductIds(Collection<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        return productAttributeQuery.findProductIdsBySkuIds(skuIds).values().stream()
                .distinct()
                .toList();
    }

    /**
     * Collaborative candidates: neighbours of products the user already engaged
     * with, scored by
     * similarity. When one product is a neighbour of several seeds, the scores add
     * - agreement across
     * seeds is a stronger signal than a single strong link.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> collaborativeScores(Collection<String> seedProductIds, int limit) {
        if (seedProductIds == null || seedProductIds.isEmpty()) {
            return Map.of();
        }
        List<ItemSimilarity> neighbours = similarityRepository.findNeighbours(seedProductIds, limit);
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        for (ItemSimilarity neighbour : neighbours) {
            scores.merge(neighbour.similarProductId(), neighbour.score().value(), BigDecimal::add);
        }
        return scores;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> popularityScores(int limit) {
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        for (ProductPopularity popularity : popularityRepository.findTop(limit)) {
            scores.put(popularity.productId(), popularity.score());
        }
        return scores;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> popularityScoresFor(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        for (ProductPopularity popularity : popularityRepository.findByProductIds(productIds)) {
            scores.put(popularity.productId(), popularity.score());
        }
        return scores;
    }

    /**
     * Content candidates: products sharing the user's preferred categories or
     * brands, scored on
     * category affinity, brand affinity and price-band fit.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> contentScores(UserAffinityProfile profile, int limit) {
        if (profile == null || profile.hasNoSignal()) {
            return Map.of();
        }
        List<ProductAttributeQueryPort.ProductAttributes> candidates = productAttributeQuery
                .findCandidatesByCategoriesOrBrands(
                        profile.getCategoryAffinities().keySet(),
                        profile.getBrandAffinities().keySet(),
                        limit);

        Map<String, BigDecimal> scores = HashMap.newHashMap(candidates.size());
        for (ProductAttributeQueryPort.ProductAttributes candidate : candidates) {
            AffinityScore category = profile.bestCategoryAffinity(candidate.categoryIds());
            AffinityScore brand = profile.brandAffinity(candidate.brandId());
            AffinityScore priceFit = profile.priceFit(candidate.priceFrom());

            BigDecimal score = category.weightedBy(weights.categoryAffinityWeight())
                    .add(brand.weightedBy(weights.brandAffinityWeight()))
                    .add(priceFit.weightedBy(weights.priceFitWeight()));
            if (score.signum() > 0) {
                scores.put(candidate.productId(), score);
            }
        }
        return scores;
    }

    /** Fallback pool for users and products with no behavioural history at all. */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> newArrivalScores(int limit) {
        List<ProductAttributeQueryPort.ProductAttributes> recent = productAttributeQuery.findRecentlyPublished(limit);
        Map<String, BigDecimal> scores = LinkedHashMap.newLinkedHashMap(recent.size());
        // Rank by recency: the port returns newest first, so a descending positional
        // score preserves
        // that order once the map is normalised.
        int rank = recent.size();
        for (ProductAttributeQueryPort.ProductAttributes product : recent) {
            scores.put(product.productId(), BigDecimal.valueOf(rank--));
        }
        return scores;
    }

    @Transactional(readOnly = true)
    public Map<String, ProductAttributeQueryPort.ProductAttributes> hydrate(
            Collection<String> productIds) {
        return productAttributeQuery.findByProductIds(productIds);
    }

    @Transactional(readOnly = true)
    public boolean productExists(String productId) {
        return productAttributeQuery.findByProductId(productId).isPresent();
    }
}
