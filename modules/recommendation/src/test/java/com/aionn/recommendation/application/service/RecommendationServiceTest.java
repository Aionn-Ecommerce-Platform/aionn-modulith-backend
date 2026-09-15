package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.policy.ColdStartPolicy;
import com.aionn.recommendation.application.policy.ColdStartThresholdPolicy;
import com.aionn.recommendation.application.policy.HybridRankingPolicy;
import com.aionn.recommendation.application.policy.RankingWeightPolicy;
import com.aionn.recommendation.application.port.out.ProductAttributeQueryPort;
import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.domain.model.UserAffinityProfile;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import com.aionn.recommendation.domain.valueobject.RecommendationReason;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Wired with the real ranking and cold-start policies rather than mocks of
 * them: what is worth testing
 * here is which signals the orchestrator asks for and what it does when a
 * surface comes back empty,
 * and both depend on the scoring arithmetic actually running.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
    private static final int LIMIT = 10;
    private static final int CANDIDATE_LIMIT = LIMIT * 3;

    @Mock
    private CandidateGenerationService candidateGeneration;
    @Mock
    private ProfileRefreshService profileRefreshService;
    @Mock
    private RecommendationMetricsPort metrics;

    private RecommendationService service() {
        return serviceWith(weights(3, 200));
    }

    private RecommendationService serviceWith(RankingWeightPolicy weights) {
        return new RecommendationService(
                candidateGeneration,
                profileRefreshService,
                new HybridRankingPolicy(),
                new ColdStartPolicy(thresholds(1, 5), weights),
                weights,
                metrics);
    }

    @Test
    void anEstablishedUserGetsABlendOfAllThreeSignalsMinusWhatTheyAlreadyOwn() {
        // Owning something is a reason not to be sold it again, even when it scores
        // highest.
        establishedProfile();
        when(candidateGeneration.recentSeedProductIds(anyString(), any(), anyInt()))
                .thenReturn(List.of("p-seed"));
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-cf", score(0.9), "p-owned", score(0.95)));
        when(candidateGeneration.contentScores(any(), anyInt())).thenReturn(Map.of("p-ct", score(0.7)));
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of("p-pop", score(100)));
        when(candidateGeneration.purchasedProductIds("user-1")).thenReturn(List.of("p-owned"));
        hydrationEchoesEveryProduct();

        assertThat(service().homeFeed("user-1", LIMIT))
                .extracting(RecommendationItemResult::productId)
                .containsExactlyInAnyOrder("p-cf", "p-ct", "p-pop");
    }

    @Test
    void aUserWithTooLittleHistoryGetsTrendingWithoutAnyPersonalisedQueries() {
        // The expensive per-user reads are skipped rather than run and discarded.
        when(profileRefreshService.profileOf("user-1"))
                .thenReturn(UserAffinityProfile.empty("user-1"));
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of("p-pop", score(50)));
        when(candidateGeneration.newArrivalScores(anyInt())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        assertThat(service().homeFeed("user-1", LIMIT))
                .extracting(RecommendationItemResult::productId)
                .containsExactly("p-pop");
        verify(candidateGeneration, never()).recentSeedProductIds(anyString(), any(), anyInt());
        verify(candidateGeneration, never()).contentScores(any(), anyInt());
    }

    @Test
    void aSlateWhoseProductsCatalogNoLongerExposesFallsBackToTrending() {
        // The interaction log keeps IDs catalog may have retired, so a ranked slate can
        // hydrate to
        // nothing. Returning that empty list would show a signed-in user a blank feed.
        establishedProfile();
        when(candidateGeneration.recentSeedProductIds(anyString(), any(), anyInt()))
                .thenReturn(List.of("p-seed"));
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-retired", score(0.9)));
        when(candidateGeneration.contentScores(any(), anyInt())).thenReturn(Map.of());
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of());
        when(candidateGeneration.purchasedProductIds("user-1")).thenReturn(List.of());
        when(candidateGeneration.newArrivalScores(anyInt())).thenReturn(Map.of("p-new", score(1)));
        hydrationEchoesEveryProductExcept("p-retired");

        assertThat(service().homeFeed("user-1", LIMIT))
                .extracting(RecommendationItemResult::productId)
                .containsExactly("p-new");
    }

    @Test
    void homeFeedFallbackPreservesPurchasedExclusions() {
        establishedProfile();
        when(candidateGeneration.recentSeedProductIds(anyString(), any(), anyInt()))
                .thenReturn(List.of("p-seed"));
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-retired", score(0.9)));
        when(candidateGeneration.contentScores(any(), anyInt())).thenReturn(Map.of());
        when(candidateGeneration.purchasedProductIds("user-1")).thenReturn(List.of("p-bought"));
        when(candidateGeneration.popularityScores(anyInt()))
                .thenReturn(Map.of("p-bought", score(10), "p-fresh", score(5)));
        hydrationEchoesEveryProductExcept("p-retired");

        assertThat(service().homeFeed("user-1", LIMIT))
                .extracting(RecommendationItemResult::productId)
                .containsExactly("p-fresh");
    }

    @Test
    void cartFallbackPreservesCartAndPurchasedExclusions() {
        when(candidateGeneration.resolveProductIds(List.of("sku-1"))).thenReturn(List.of("p-cart"));
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-retired", score(0.9)));
        when(candidateGeneration.purchasedProductIds("user-1")).thenReturn(List.of("p-bought"));
        when(candidateGeneration.popularityScores(anyInt()))
                .thenReturn(Map.of("p-cart", score(10), "p-bought", score(8), "p-fresh", score(5)));
        hydrationEchoesEveryProductExcept("p-retired");

        assertThat(service().cartSuggestions("user-1", List.of("sku-1"), LIMIT))
                .extracting(RecommendationItemResult::productId)
                .containsExactly("p-fresh");
    }

    @Test
    void productSurfaceFallbackPreservesSeedExclusion() {
        when(candidateGeneration.productExists("p-seed")).thenReturn(true);
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-retired", score(0.9)));
        when(candidateGeneration.popularityScores(anyInt()))
                .thenReturn(Map.of("p-seed", score(10), "p-fresh", score(5)));
        hydrationEchoesEveryProductExcept("p-retired");

        assertThat(service().similarProducts("p-seed", LIMIT))
                .extracting(RecommendationItemResult::productId)
                .containsExactly("p-fresh");
    }

    @Test
    void aProductSurfaceRestrictsPopularityToTheNeighbourSet() {
        // Popularity is there to break ties between neighbours. Drawing it from the
        // global pool would
        // put unrelated best-sellers on a "similar products" shelf.
        when(candidateGeneration.productExists("p-1")).thenReturn(true);
        when(candidateGeneration.collaborativeScores(eq(List.of("p-1")), anyInt()))
                .thenReturn(Map.of("p-n1", score(0.8), "p-n2", score(0.6)));
        when(candidateGeneration.popularityScoresFor(anyCollection()))
                .thenReturn(Map.of("p-n1", score(10)));
        hydrationEchoesEveryProduct();

        assertThat(service().similarProducts("p-1", LIMIT))
                .extracting(RecommendationItemResult::productId)
                .containsExactlyInAnyOrder("p-n1", "p-n2");
        verify(candidateGeneration, never()).popularityScores(anyInt());
    }

    @Test
    void aProductWithNoCoOccurrenceDataYetDrawsOnTheGlobalPopularityPool() {
        // A freshly seeded catalog has no similarity rows at all, and an empty shelf is
        // worse than a
        // popular one.
        when(candidateGeneration.productExists("p-1")).thenReturn(true);
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt())).thenReturn(Map.of());
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of("p-pop", score(5)));
        hydrationEchoesEveryProduct();

        assertThat(service().similarProducts("p-1", LIMIT))
                .extracting(RecommendationItemResult::productId)
                .containsExactly("p-pop");
        verify(candidateGeneration, never()).popularityScoresFor(anyCollection());
    }

    @Test
    void anUnknownProductGetsTrendingRatherThanAnError() {
        when(candidateGeneration.productExists("p-missing")).thenReturn(false);
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of("p-pop", score(5)));
        when(candidateGeneration.newArrivalScores(anyInt())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        assertThat(service().similarProducts("p-missing", LIMIT)).isNotEmpty();
        verify(candidateGeneration, never()).collaborativeScores(anyCollection(), anyInt());
    }

    @Test
    void theSameArithmeticIsExplainedDifferentlyOnTheAlsoBoughtSurface() {
        // Identical inputs to the similar-products case; only the surface differs, and
        // co-occurrence
        // has a more specific name there.
        when(candidateGeneration.productExists("p-1")).thenReturn(true);
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-n1", score(0.8)));
        when(candidateGeneration.popularityScoresFor(anyCollection())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        assertThat(service().alsoBought("p-1", LIMIT))
                .extracting(RecommendationItemResult::reason)
                .containsExactly(RecommendationReason.FREQUENTLY_BOUGHT_TOGETHER);
        assertThat(service().similarProducts("p-1", LIMIT))
                .extracting(RecommendationItemResult::reason)
                .containsExactly(RecommendationReason.SIMILAR_TO_VIEWED);
    }

    @Test
    void cartSuggestionsExcludeTheBasketItself() {
        when(candidateGeneration.resolveProductIds(List.of("sku-1"))).thenReturn(List.of("p-in-cart"));
        when(candidateGeneration.collaborativeScores(eq(List.of("p-in-cart")), anyInt()))
                .thenReturn(Map.of("p-in-cart", score(0.9), "p-complement", score(0.5)));
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of());
        when(candidateGeneration.purchasedProductIds("user-1")).thenReturn(List.of());
        hydrationEchoesEveryProduct();

        assertThat(service().cartSuggestions("user-1", List.of("sku-1"), LIMIT))
                .extracting(RecommendationItemResult::productId)
                .containsExactly("p-complement");
    }

    @Test
    void anAnonymousBasketIsNotCheckedAgainstPurchaseHistory() {
        // There is no history to check, and a null user ID would query across every
        // user's purchases.
        when(candidateGeneration.resolveProductIds(anyCollection())).thenReturn(List.of("p-in-cart"));
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-complement", score(0.5)));
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        assertThat(service().cartSuggestions(null, List.of("sku-1"), LIMIT)).isNotEmpty();
        verify(candidateGeneration, never()).purchasedProductIds(any());
    }

    @Test
    void aBasketWhoseSkusResolveToNothingFallsBackToTheHomeFeed() {
        when(candidateGeneration.resolveProductIds(anyCollection())).thenReturn(List.of());
        when(profileRefreshService.profileOf("user-1"))
                .thenReturn(UserAffinityProfile.empty("user-1"));
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of("p-pop", score(5)));
        when(candidateGeneration.newArrivalScores(anyInt())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        assertThat(service().cartSuggestions("user-1", List.of("sku-unknown"), LIMIT)).isNotEmpty();
    }

    @Test
    void newArrivalsTopUpTrendingWhileMomentumDataIsThin() {
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of("p-pop", score(5)));
        when(candidateGeneration.newArrivalScores(anyInt())).thenReturn(Map.of("p-new", score(1)));
        hydrationEchoesEveryProduct();

        assertThat(service().trending(LIMIT))
                .extracting(RecommendationItemResult::productId)
                .contains("p-new");
    }

    @Test
    void aFullPopularityPoolNeedsNoNewArrivals() {
        // Catalog is queried only when behaviour cannot fill the slate; once it can,
        // the extra read is
        // pure cost.
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(popularityPool(CANDIDATE_LIMIT));
        hydrationEchoesEveryProduct();

        assertThat(service().trending(LIMIT)).hasSize(CANDIDATE_LIMIT);
        verify(candidateGeneration, never()).newArrivalScores(anyInt());
    }

    @Test
    void candidatesAreOverFetchedSoDownstreamAvailabilityFilteringStillLeavesAFullPage() {
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of());
        when(candidateGeneration.newArrivalScores(anyInt())).thenReturn(Map.of());

        service().trending(LIMIT);

        verify(candidateGeneration).popularityScores(CANDIDATE_LIMIT);
    }

    @Test
    void overFetchingIsCappedSoALargePageCannotScanTheWholeCatalog() {
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of());
        when(candidateGeneration.newArrivalScores(anyInt())).thenReturn(Map.of());

        serviceWith(weights(3, 25)).trending(LIMIT);

        verify(candidateGeneration).popularityScores(25);
    }

    @Test
    void aColdStartHomeFeedCountsAsATrendingFallback() {
        // Counting cold starts too is deliberate: a profile pipeline that stopped building profiles looks
        // identical to a catalogue of brand-new users from the response alone, and this counter is the
        // only place the difference shows up.
        when(profileRefreshService.profileOf("user-1"))
                .thenReturn(UserAffinityProfile.empty("user-1"));
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of("p-pop", score(50)));
        when(candidateGeneration.newArrivalScores(anyInt())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        service().homeFeed("user-1", LIMIT);

        verify(metrics).recordTrendingFallback(RecommendationSurface.HOME);
    }

    @Test
    void anEmptyPersonalisedSlateCountsAsATrendingFallback() {
        establishedProfile();
        when(candidateGeneration.recentSeedProductIds(anyString(), any(), anyInt()))
                .thenReturn(List.of("p-seed"));
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-retired", score(0.9)));
        when(candidateGeneration.contentScores(any(), anyInt())).thenReturn(Map.of());
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of());
        when(candidateGeneration.purchasedProductIds("user-1")).thenReturn(List.of());
        when(candidateGeneration.newArrivalScores(anyInt())).thenReturn(Map.of("p-new", score(1)));
        hydrationEchoesEveryProductExcept("p-retired");

        service().homeFeed("user-1", LIMIT);

        verify(metrics).recordTrendingFallback(RecommendationSurface.HOME);
    }

    @Test
    void aFallbackIsCountedAgainstTheSurfaceBeingServedNotAlwaysTheHomeFeed() {
        // The tag is what makes the counter actionable: "trending fallbacks went up" is useless if it
        // cannot say whether similar-products or the home feed stopped producing results.
        when(candidateGeneration.productExists("p-missing")).thenReturn(false);
        when(candidateGeneration.popularityScores(anyInt())).thenReturn(Map.of("p-pop", score(5)));
        when(candidateGeneration.newArrivalScores(anyInt())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        service().alsoBought("p-missing", LIMIT);

        verify(metrics).recordTrendingFallback(RecommendationSurface.ALSO_BOUGHT);
        verify(metrics, never()).recordTrendingFallback(RecommendationSurface.HOME);
    }

    @Test
    void aSurfaceThatProducesResultsIsNotCountedAsAFallback() {
        when(candidateGeneration.productExists("p-1")).thenReturn(true);
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-2", score(0.9)));
        when(candidateGeneration.popularityScoresFor(anyCollection())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        assertThat(service().similarProducts("p-1", LIMIT)).isNotEmpty();

        verify(metrics, never()).recordTrendingFallback(any());
    }

    @Test
    void anExtremePageSizeDoesNotOverflowTheCandidateLimit() {
        // limit * overFetchFactor overflows int for a large enough page and comes back negative, which
        // then fails every ArrayList built from it. Clamping to the largest page any surface builds keeps
        // the product positive: 50 * 3 rather than something near Integer.MIN_VALUE.
        when(candidateGeneration.productExists("p-1")).thenReturn(true);
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-2", score(0.9)));
        when(candidateGeneration.popularityScoresFor(anyCollection())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        serviceWith(weights(3, 200)).similarProducts("p-1", Integer.MAX_VALUE);

        verify(candidateGeneration).collaborativeScores(anyCollection(), eq(150));
    }

    @Test
    void theOverFetchIsStillCappedAtTheConfiguredMaximum() {
        // The cap is what stops a full-size page from scanning the whole catalogue: 50 * 5 candidates is
        // more than the configured ceiling, so the ceiling wins.
        when(candidateGeneration.productExists("p-1")).thenReturn(true);
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-2", score(0.9)));
        when(candidateGeneration.popularityScoresFor(anyCollection())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        serviceWith(weights(5, 200)).similarProducts("p-1", 50);

        verify(candidateGeneration).collaborativeScores(anyCollection(), eq(200));
    }

    @Test
    void aNonPositivePageSizeStillAsksForAtLeastOneCandidate() {
        when(candidateGeneration.productExists("p-1")).thenReturn(true);
        when(candidateGeneration.collaborativeScores(anyCollection(), anyInt()))
                .thenReturn(Map.of("p-2", score(0.9)));
        when(candidateGeneration.popularityScoresFor(anyCollection())).thenReturn(Map.of());
        hydrationEchoesEveryProduct();

        serviceWith(weights(3, 200)).similarProducts("p-1", -5);

        // One page times the over-fetch factor, not a negative candidate count.
        verify(candidateGeneration).collaborativeScores(anyCollection(), eq(3));
    }

    private void establishedProfile() {
        when(profileRefreshService.profileOf("user-1")).thenReturn(new UserAffinityProfile(
                "user-1",
                Map.of("cat-phone", AffinityScore.ONE),
                Map.of("brand-apple", AffinityScore.ONE),
                null,
                null,
                20,
                NOW));
    }

    private void hydrationEchoesEveryProduct() {
        hydrationEchoesEveryProductExcept(null);
    }

    private void hydrationEchoesEveryProductExcept(String retiredProductId) {
        when(candidateGeneration.hydrate(anyCollection())).thenAnswer(invocation -> {
            Collection<String> productIds = invocation.getArgument(0);
            Map<String, ProductAttributeQueryPort.ProductAttributes> attributes = new LinkedHashMap<>();
            for (String productId : productIds) {
                if (!productId.equals(retiredProductId)) {
                    attributes.put(productId, attributes(productId));
                }
            }
            return attributes;
        });
    }

    private static Map<String, BigDecimal> popularityPool(int size) {
        Map<String, BigDecimal> pool = new LinkedHashMap<>(size);
        IntStream.range(0, size).forEach(index -> pool.put("p-pop-" + index, score(size - index)));
        return pool;
    }

    private static ProductAttributeQueryPort.ProductAttributes attributes(String productId) {
        return new ProductAttributeQueryPort.ProductAttributes(
                productId,
                "Product " + productId,
                "brand-apple",
                List.of("cat-phone"),
                List.of("sku-" + productId),
                null,
                BigDecimal.valueOf(1_000_000),
                "VND");
    }

    private static BigDecimal score(double value) {
        return BigDecimal.valueOf(value);
    }

    private static ColdStartThresholdPolicy thresholds(int contentOnly, int fullHybrid) {
        return new ColdStartThresholdPolicy() {
            @Override
            public int contentOnlyThreshold() {
                return contentOnly;
            }

            @Override
            public int fullHybridThreshold() {
                return fullHybrid;
            }
        };
    }

    private static RankingWeightPolicy weights(int overFetchFactor, int maxCandidates) {
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
                return overFetchFactor;
            }

            @Override
            public int maxCandidates() {
                return maxCandidates;
            }
        };
    }
}
