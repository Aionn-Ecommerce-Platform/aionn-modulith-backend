package com.aionn.recommendation.application.policy;

import com.aionn.recommendation.domain.model.RecommendationSlate;
import com.aionn.recommendation.domain.valueobject.RecommendationReason;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HybridRankingPolicyTest {

        private final HybridRankingPolicy policy = new HybridRankingPolicy();

        @Test
        void popularityCannotOutweighItsConfiguredShare() {
                // The core reason every signal is normalised first: raw popularity can be in
                // the thousands while
                // a cosine never exceeds one. Un-normalised, the 0.15 popularity weight would
                // still decide the
                // ranking outright.
                HybridRankingPolicy.SignalScores signals = new HybridRankingPolicy.SignalScores(
                                Map.of("p-cf", BigDecimal.valueOf(0.9)),
                                Map.of(),
                                Map.of("p-pop", BigDecimal.valueOf(5000), "p-cf", BigDecimal.valueOf(1)));

                RecommendationSlate slate = policy.rank(
                                RecommendationSurface.HOME,
                                signals,
                                weights(0.50, 0.35, 0.15),
                                Set.of(),
                                10);

                assertThat(slate.productIds().getFirst()).isEqualTo("p-cf");
        }

        @Test
        void aSignalWithZeroWeightContributesNothing() {
                HybridRankingPolicy.SignalScores signals = new HybridRankingPolicy.SignalScores(
                                Map.of("p-cf", BigDecimal.ONE),
                                Map.of(),
                                Map.of("p-pop", BigDecimal.ONE));

                RecommendationSlate slate = policy.rank(
                                RecommendationSurface.HOME,
                                signals,
                                weights(0.0, 0.0, 1.0),
                                Set.of(),
                                10);

                assertThat(slate.productIds()).containsExactly("p-pop");
        }

        @Test
        void excludedProductsNeverAppear() {
                HybridRankingPolicy.SignalScores signals = new HybridRankingPolicy.SignalScores(
                                Map.of("p-owned", BigDecimal.ONE, "p-new", BigDecimal.valueOf(0.5)),
                                Map.of(),
                                Map.of());

                RecommendationSlate slate = policy.rank(
                                RecommendationSurface.HOME,
                                signals,
                                weights(1.0, 0.0, 0.0),
                                Set.of("p-owned"),
                                10);

                assertThat(slate.productIds()).containsExactly("p-new");
        }

        @Test
        void theDominantSignalDeterminesTheStatedReason() {
                HybridRankingPolicy.SignalScores collaborativeLed = new HybridRankingPolicy.SignalScores(
                                Map.of("p-1", BigDecimal.ONE, "p-2", BigDecimal.ZERO), Map.of(), Map.of());
                HybridRankingPolicy.SignalScores contentLed = new HybridRankingPolicy.SignalScores(
                                Map.of(), Map.of("p-1", BigDecimal.ONE, "p-2", BigDecimal.ZERO), Map.of());

                RecommendationSlate fromCollaborative = policy.rank(
                                RecommendationSurface.HOME, collaborativeLed, weights(1.0, 0.0, 0.0), Set.of(), 1);
                RecommendationSlate fromContent = policy.rank(
                                RecommendationSurface.HOME, contentLed, weights(0.0, 1.0, 0.0), Set.of(), 1);

                assertThat(fromCollaborative.items().getFirst().reason())
                                .isEqualTo(RecommendationReason.SIMILAR_TO_VIEWED);
                assertThat(fromContent.items().getFirst().reason())
                                .isEqualTo(RecommendationReason.MATCHES_YOUR_INTERESTS);
        }

        @Test
        void theAlsoBoughtSurfaceNamesCoOccurrenceMoreSpecifically() {
                HybridRankingPolicy.SignalScores signals = new HybridRankingPolicy.SignalScores(
                                Map.of("p-1", BigDecimal.ONE, "p-2", BigDecimal.ZERO), Map.of(), Map.of());

                RecommendationSlate slate = policy.rank(
                                RecommendationSurface.ALSO_BOUGHT, signals, weights(1.0, 0.0, 0.0), Set.of(), 1);

                assertThat(slate.items().getFirst().reason())
                                .isEqualTo(RecommendationReason.FREQUENTLY_BOUGHT_TOGETHER);
        }

        @Test
        void weightsSummingToZeroYieldAnEmptySlate() {
                HybridRankingPolicy.SignalScores signals = new HybridRankingPolicy.SignalScores(
                                Map.of("p-1", BigDecimal.ONE), Map.of(), Map.of());

                RecommendationSlate slate = policy.rank(
                                RecommendationSurface.HOME, signals, weights(0.0, 0.0, 0.0), Set.of(), 10);

                assertThat(slate.isEmpty()).isTrue();
        }

        @Test
        void agreementAcrossSignalsRanksAboveASingleStrongSignal() {
                HybridRankingPolicy.SignalScores signals = new HybridRankingPolicy.SignalScores(
                                Map.of("p-both", BigDecimal.valueOf(0.6), "p-cfOnly", BigDecimal.ONE),
                                Map.of("p-both", BigDecimal.ONE, "p-contentOnly", BigDecimal.valueOf(0.6)),
                                Map.of());

                RecommendationSlate slate = policy.rank(
                                RecommendationSurface.HOME, signals, weights(0.5, 0.5, 0.0), Set.of(), 10);

                assertThat(slate.productIds().getFirst()).isEqualTo("p-both");
        }

        @Test
        void everyScoreStaysWithinTheUnitRange() {
                HybridRankingPolicy.SignalScores signals = new HybridRankingPolicy.SignalScores(
                                Map.of("p-1", BigDecimal.valueOf(10), "p-2", BigDecimal.ONE),
                                Map.of("p-1", BigDecimal.valueOf(99)),
                                Map.of("p-1", BigDecimal.valueOf(1234)));

                RecommendationSlate slate = policy.rank(
                                RecommendationSurface.HOME, signals, weights(0.5, 0.35, 0.15), Set.of(), 10);

                List<BigDecimal> scores = slate.items().stream()
                                .map(item -> item.score().value())
                                .toList();
                assertThat(scores).isNotEmpty().allSatisfy(score -> assertThat(score)
                                .isBetween(BigDecimal.ZERO, BigDecimal.ONE));
        }

        private static ColdStartPolicy.SignalWeights weights(
                        double collaborative, double content, double popularity) {
                return new ColdStartPolicy.SignalWeights(
                                BigDecimal.valueOf(collaborative),
                                BigDecimal.valueOf(content),
                                BigDecimal.valueOf(popularity));
        }
}
