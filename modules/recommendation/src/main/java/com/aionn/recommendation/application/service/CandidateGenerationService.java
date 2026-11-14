package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.policy.RankingWeightPolicy;
import com.aionn.recommendation.application.policy.RecommendationServingPolicy;
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
import java.util.Optional;

/** Builds the raw signal maps consumed by hybrid ranking, each read in its own short read-only transaction. */
@Service
@RequiredArgsConstructor
public class CandidateGenerationService {

    private final InteractionPersistencePort interactionPersistencePort;
    private final ItemSimilarityPersistencePort itemSimilarityPersistencePort;
    private final PopularityPersistencePort popularityPersistencePort;
    private final ProductAttributeQueryPort productAttributeQuery;
    private final RankingWeightPolicy weights;
    private final RecommendationServingPolicy servingPolicy;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<String> recentSeedProductIds(String userId, Duration lookback, int limit) {
        return interactionPersistencePort.findByUser(userId, clock.instant().minus(lookback), limit)
                .stream()
                .map(UserInteraction::getProductId)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> purchasedProductIds(String userId) {
        return interactionPersistencePort.findPurchasedProductIds(userId);
    }

    @Transactional(readOnly = true)
    public List<String> resolveProductIds(Collection<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        return productAttributeQuery.findProductIdsBySkuIds(skuIds).values().stream()
                .distinct()
                .toList();
    }

    /** Similarity scores accumulate across seeds: agreement is a stronger signal than one link. */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> collaborativeScores(Collection<String> seedProductIds, int limit) {
        if (seedProductIds == null || seedProductIds.isEmpty()) {
            return Map.of();
        }
        List<ItemSimilarity> neighbours = itemSimilarityPersistencePort.findNeighbours(seedProductIds, limit);
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        for (ItemSimilarity neighbour : neighbours) {
            scores.merge(neighbour.similarProductId(), neighbour.score().value(), BigDecimal::add);
        }
        return scores;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> popularityScores(int limit) {
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        for (ProductPopularity popularity : popularityPersistencePort.findTop(limit)) {
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
        for (ProductPopularity popularity : popularityPersistencePort.findByProductIds(productIds)) {
            scores.put(popularity.productId(), popularity.score());
        }
        return scores;
    }

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
        // Port returns newest first, so a descending positional score preserves recency.
        int rank = recent.size();
        for (ProductAttributeQueryPort.ProductAttributes product : recent) {
            scores.put(product.productId(), BigDecimal.valueOf(rank--));
        }
        return scores;
    }

    /** Fallback pool for product surfaces with no behavioural co-occurrence. */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> categoryCandidates(String productId, int limit) {
        Optional<ProductAttributeQueryPort.ProductAttributes> targetOpt = productAttributeQuery.findByProductId(productId);
        if (targetOpt.isEmpty()) {
            return Map.of();
        }
        ProductAttributeQueryPort.ProductAttributes target = targetOpt.get();
        List<String> categories = target.categoryIds();
        if (categories == null || categories.isEmpty()) {
            return Map.of();
        }

        // Match any category: catalog does not guarantee ordering, so don't infer specificity.
        List<ProductAttributeQueryPort.ProductAttributes> candidates = productAttributeQuery
                .findCandidatesByCategoriesOrBrands(categories, List.of(),
                        limit + servingPolicy.categoryCandidateOvershoot());

        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        int rank = candidates.size();
        for (ProductAttributeQueryPort.ProductAttributes candidate : candidates) {
            if (!candidate.productId().equals(productId)) {
                scores.put(candidate.productId(), BigDecimal.valueOf(rank--));
            }
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
