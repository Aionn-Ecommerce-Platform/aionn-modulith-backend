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
import com.aionn.recommendation.domain.valueobject.InteractionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateGenerationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    @Mock
    private InteractionPersistencePort interactionRepository;
    @Mock
    private ItemSimilarityPersistencePort similarityRepository;
    @Mock
    private PopularityPersistencePort popularityRepository;
    @Mock
    private ProductAttributeQueryPort productAttributeQuery;

    private CandidateGenerationService service() {
        return new CandidateGenerationService(
                interactionRepository,
                similarityRepository,
                popularityRepository,
                productAttributeQuery,
                weights(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void neighbourScoresAddUpAcrossSeeds() {
        // A product reached from several seeds is a stronger candidate than one reached
        // from a single
        // seed, however similar that one link is.
        when(similarityRepository.findNeighbours(anyCollection(), anyInt())).thenReturn(List.of(
                similarity("p-seed-a", "p-shared", 0.4),
                similarity("p-seed-b", "p-shared", 0.4),
                similarity("p-seed-a", "p-single", 0.7)));

        Map<String, BigDecimal> scores = service().collaborativeScores(List.of("p-seed-a", "p-seed-b"), 50);

        assertThat(scores.get("p-shared")).isGreaterThan(scores.get("p-single"));
    }

    @Test
    void noSeedsMeansNoCollaborativeCandidates() {
        assertThat(service().collaborativeScores(List.of(), 50)).isEmpty();
        assertThat(service().collaborativeScores(null, 50)).isEmpty();
    }

    @Test
    void contentScoreCombinesCategoryBrandAndPriceFit() {
        UserAffinityProfile profile = new UserAffinityProfile(
                "user-1",
                Map.of("cat-phone", AffinityScore.ONE),
                Map.of("brand-apple", AffinityScore.ONE),
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(40_000_000),
                20,
                NOW);
        when(productAttributeQuery.findCandidatesByCategoriesOrBrands(
                anyCollection(), anyCollection(), anyInt())).thenReturn(List.of(
                        product("p-perfect", "brand-apple", List.of("cat-phone"), 30_000_000),
                        product("p-category-only", null, List.of("cat-phone"), 900_000_000)));

        Map<String, BigDecimal> scores = service().contentScores(profile, 50);

        assertThat(scores.get("p-perfect")).isGreaterThan(scores.get("p-category-only"));
    }

    @Test
    void candidatesMatchingNothingAreLeftOutEntirely() {
        UserAffinityProfile profile = new UserAffinityProfile(
                "user-1", Map.of("cat-phone", AffinityScore.ONE), Map.of(), null, null, 20, NOW);
        when(productAttributeQuery.findCandidatesByCategoriesOrBrands(
                anyCollection(), anyCollection(), anyInt())).thenReturn(List.of(
                        product("p-unrelated", "brand-other", List.of("cat-other"), 1_000_000)));

        assertThat(service().contentScores(profile, 50)).isEmpty();
    }

    @Test
    void aProfileWithNoSignalProducesNoContentCandidates() {
        assertThat(service().contentScores(UserAffinityProfile.empty("user-1"), 50)).isEmpty();
        assertThat(service().contentScores(null, 50)).isEmpty();
    }

    @Test
    void newArrivalsAreScoredByRecencyOrder() {
        // The port returns newest first; a descending positional score preserves that
        // once normalised.
        when(productAttributeQuery.findRecentlyPublished(anyInt())).thenReturn(List.of(
                product("p-newest", "b", List.of("cat-1"), 1_000_000),
                product("p-older", "b", List.of("cat-1"), 1_000_000)));

        Map<String, BigDecimal> scores = service().newArrivalScores(50);

        assertThat(scores.get("p-newest")).isGreaterThan(scores.get("p-older"));
    }

    @Test
    void popularityScoresArePassedThroughUnchanged() {
        when(popularityRepository.findTop(anyInt())).thenReturn(List.of(
                popularity("p-1", 120.5), popularity("p-2", 3.0)));

        Map<String, BigDecimal> scores = service().popularityScores(50);

        assertThat(scores.get("p-1")).isEqualByComparingTo(BigDecimal.valueOf(120.5));
        assertThat(scores.get("p-2")).isEqualByComparingTo(BigDecimal.valueOf(3.0));
    }

    @Test
    void cartSkusResolveToDistinctProducts() {
        // Two variants of one product must not seed the same product twice.
        when(productAttributeQuery.findProductIdsBySkuIds(anyCollection()))
                .thenReturn(Map.of("sku-1", "p-1", "sku-2", "p-1", "sku-3", "p-2"));

        assertThat(service().resolveProductIds(List.of("sku-1", "sku-2", "sku-3")))
                .containsExactlyInAnyOrder("p-1", "p-2");
    }

    @Test
    void anEmptyBasketIsNotSentToTheCatalog() {
        assertThat(service().resolveProductIds(List.of())).isEmpty();
        assertThat(service().resolveProductIds(null)).isEmpty();
        verify(productAttributeQuery, never()).findProductIdsBySkuIds(anyCollection());
    }

    @Test
    void seedsAreTheDistinctProductsTouchedInsideTheLookbackWindow() {
        // The lookback is applied against the fixed clock, and a user who viewed one
        // product five times
        // seeds it once - a repeated view is not five separate pieces of evidence for
        // the CF lookup.
        when(interactionRepository.findByUser("user-1", NOW.minus(Duration.ofDays(30)), 20))
                .thenReturn(List.of(
                        interaction("p-1"), interaction("p-1"), interaction("p-2")));

        assertThat(service().recentSeedProductIds("user-1", Duration.ofDays(30), 20))
                .containsExactly("p-1", "p-2");
    }

    @Test
    void purchasesArePassedStraightThroughForExclusion() {
        when(interactionRepository.findPurchasedProductIds("user-1")).thenReturn(List.of("p-owned"));

        assertThat(service().purchasedProductIds("user-1")).containsExactly("p-owned");
    }

    @Test
    void popularityCanBeRestrictedToAGivenSetOfProducts() {
        // Used on product surfaces, where popularity breaks ties between neighbours
        // instead of
        // introducing unrelated best-sellers.
        when(popularityRepository.findByProductIds(anyCollection()))
                .thenReturn(List.of(popularity("p-1", 9.0)));

        assertThat(service().popularityScoresFor(List.of("p-1", "p-2")))
                .containsOnlyKeys("p-1");
    }

    @Test
    void anEmptyProductSetSkipsTheQuery() {
        assertThat(service().popularityScoresFor(List.of())).isEmpty();
        assertThat(service().popularityScoresFor(null)).isEmpty();
        verify(popularityRepository, never()).findByProductIds(anyCollection());
    }

    @Test
    void hydrationAndExistenceChecksDelegateToTheCatalog() {
        // Both exist so the orchestrator can stay non-transactional: each opens its own
        // short read-only
        // transaction here.
        when(productAttributeQuery.findByProductIds(anyCollection()))
                .thenReturn(Map.of("p-1", product("p-1", "b", List.of("cat-1"), 1_000_000)));
        when(productAttributeQuery.findByProductId("p-1"))
                .thenReturn(Optional.of(product("p-1", "b", List.of("cat-1"), 1_000_000)));
        when(productAttributeQuery.findByProductId("p-gone")).thenReturn(Optional.empty());

        assertThat(service().hydrate(List.of("p-1"))).containsOnlyKeys("p-1");
        assertThat(service().productExists("p-1")).isTrue();
        assertThat(service().productExists("p-gone")).isFalse();
    }

    private static UserInteraction interaction(String productId) {
        return UserInteraction.create(
                "int-" + productId, "user-1", productId, InteractionType.VIEW, BigDecimal.ONE, NOW);
    }

    private static ItemSimilarity similarity(String productId, String similarTo, double score) {
        return new ItemSimilarity(
                productId, similarTo, AffinityScore.of(score), 3, NOW);
    }

    private static ProductPopularity popularity(String productId, double score) {
        return new ProductPopularity(productId, BigDecimal.valueOf(score), 10, 2, NOW);
    }

    private static ProductAttributeQueryPort.ProductAttributes product(
            String productId, String brandId, List<String> categoryIds, long price) {
        return new ProductAttributeQueryPort.ProductAttributes(
                productId,
                "Product " + productId,
                brandId,
                categoryIds,
                List.of("sku-" + productId),
                null,
                BigDecimal.valueOf(price),
                "VND");
    }

    private static RankingWeightPolicy weights() {
        return new RankingWeightPolicy() {
            @Override
            public BigDecimal collaborativeWeight() {
                return BigDecimal.valueOf(0.50);
            }

            @Override
            public BigDecimal contentWeight() {
                return BigDecimal.valueOf(0.35);
            }

            @Override
            public BigDecimal popularityWeight() {
                return BigDecimal.valueOf(0.15);
            }

            @Override
            public BigDecimal categoryAffinityWeight() {
                return BigDecimal.valueOf(0.45);
            }

            @Override
            public BigDecimal brandAffinityWeight() {
                return BigDecimal.valueOf(0.35);
            }

            @Override
            public BigDecimal priceFitWeight() {
                return BigDecimal.valueOf(0.20);
            }

            @Override
            public int candidateOverFetchFactor() {
                return 3;
            }

            @Override
            public int maxCandidates() {
                return 200;
            }
        };
    }
}
