package com.aionn.recommendation.domain.model;

import com.aionn.recommendation.domain.valueobject.AffinityScore;
import com.aionn.recommendation.domain.valueobject.RecommendationReason;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecommendationSlateTest {

    @Test
    void candidatesAreOrderedByScoreDescending() {
        RecommendationSlate slate = RecommendationSlate.of(
                RecommendationSurface.HOME,
                List.of(scored("p-low", 0.2), scored("p-high", 0.9), scored("p-mid", 0.5)),
                10);

        assertThat(slate.productIds()).containsExactly("p-high", "p-mid", "p-low");
    }

    @Test
    void theHighestScoringArrivalOfADuplicateWins() {
        // The same product legitimately arrives from several candidate sources; it should appear once,
        // carrying its best score and that source's reason.
        RecommendationSlate slate = RecommendationSlate.of(
                RecommendationSurface.HOME,
                List.of(
                        new RecommendationSlate.ScoredProduct(
                                "p-1", AffinityScore.of(0.3), RecommendationReason.TRENDING),
                        new RecommendationSlate.ScoredProduct(
                                "p-1", AffinityScore.of(0.8), RecommendationReason.SIMILAR_TO_VIEWED)),
                10);

        assertThat(slate.items()).hasSize(1);
        assertThat(slate.items().getFirst().score().value())
                .isEqualByComparingTo(java.math.BigDecimal.valueOf(0.8));
        assertThat(slate.items().getFirst().reason())
                .isEqualTo(RecommendationReason.SIMILAR_TO_VIEWED);
    }

    @Test
    void tiesBreakOnProductIdSoTheOrderIsStable() {
        // An unstable order would make cached and freshly computed responses disagree for no reason.
        RecommendationSlate first = RecommendationSlate.of(
                RecommendationSurface.HOME,
                List.of(scored("p-b", 0.5), scored("p-a", 0.5), scored("p-c", 0.5)),
                10);
        RecommendationSlate second = RecommendationSlate.of(
                RecommendationSurface.HOME,
                List.of(scored("p-c", 0.5), scored("p-b", 0.5), scored("p-a", 0.5)),
                10);

        assertThat(first.productIds()).containsExactly("p-a", "p-b", "p-c");
        assertThat(second.productIds()).isEqualTo(first.productIds());
    }

    @Test
    void theSlateIsTruncatedToTheLimit() {
        RecommendationSlate slate = RecommendationSlate.of(
                RecommendationSurface.HOME,
                List.of(scored("p-1", 0.9), scored("p-2", 0.8), scored("p-3", 0.7)),
                2);

        assertThat(slate.productIds()).containsExactly("p-1", "p-2");
    }

    @Test
    void aNonPositiveLimitYieldsAnEmptySlate() {
        RecommendationSlate slate = RecommendationSlate.of(
                RecommendationSurface.SIMILAR, List.of(scored("p-1", 0.9)), 0);

        assertThat(slate.isEmpty()).isTrue();
        assertThat(slate.surface()).isEqualTo(RecommendationSurface.SIMILAR);
    }

    @Test
    void noCandidatesYieldsAnEmptySlateForTheSameSurface() {
        assertThat(RecommendationSlate.of(RecommendationSurface.CART, List.of(), 10).isEmpty()).isTrue();
        assertThat(RecommendationSlate.of(RecommendationSurface.CART, null, 10).surface())
                .isEqualTo(RecommendationSurface.CART);
    }

    @Test
    void aSlateWithoutASurfaceIsRejected() {
        // Surface decides the cache, the candidate sources and the wording of the reason; a slate that
        // does not know its own surface cannot be served.
        assertThatThrownBy(() -> new RecommendationSlate(null, List.of()))
                .hasMessageContaining("surface must not be null");
    }

    @Test
    void anIncompletelyScoredCandidateIsRejected() {
        // These are assembled by the ranking policy, so a null here means an arithmetic path produced no
        // score or no explanation - both of which would reach the client as a broken item.
        assertThatThrownBy(() -> new RecommendationSlate.ScoredProduct(
                " ", AffinityScore.ONE, RecommendationReason.TRENDING))
                .hasMessageContaining("productId must not be blank");
        assertThatThrownBy(() -> new RecommendationSlate.ScoredProduct(
                "p-1", null, RecommendationReason.TRENDING))
                .hasMessageContaining("score must not be null");
        assertThatThrownBy(() -> new RecommendationSlate.ScoredProduct("p-1", AffinityScore.ONE, null))
                .hasMessageContaining("reason must not be null");
    }

    private static RecommendationSlate.ScoredProduct scored(String productId, double score) {
        return new RecommendationSlate.ScoredProduct(
                productId, AffinityScore.of(score), RecommendationReason.TRENDING);
    }
}
