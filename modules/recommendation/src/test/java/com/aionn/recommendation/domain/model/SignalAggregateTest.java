package com.aionn.recommendation.domain.model;

import com.aionn.recommendation.domain.valueobject.AffinityScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignalAggregateTest {

    private static final Instant COMPUTED_AT = Instant.parse("2026-09-05T12:00:00Z");

    @Test
    void aSimilarityRowCarriesTheCoOccurrenceCountAlongsideTheScore() {
        // A cosine of 1.0 over two shared users is noise; callers cannot tell the difference from the
        // score alone, so the evidence count travels with it.
        ItemSimilarity similarity = new ItemSimilarity(
                "p-1", "p-2", AffinityScore.of(1.0), 2, COMPUTED_AT);

        assertThat(similarity.coOccurrence()).isEqualTo(2);
        assertThat(similarity.score().value()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void aProductIsNeverSimilarToItself() {
        // The SQL pair-building step already excludes it; this is the same invariant enforced where the
        // rows are read back, so a hand-written row cannot slip past.
        assertThatThrownBy(() -> new ItemSimilarity(
                "p-1", "p-1", AffinityScore.of(0.9), 5, COMPUTED_AT))
                .hasMessageContaining("cannot be similar to itself");
    }

    @Test
    void aSimilarityWithoutEvidenceIsRejected() {
        assertThatThrownBy(() -> new ItemSimilarity(
                "p-1", "p-2", AffinityScore.of(0.9), 0, COMPUTED_AT))
                .hasMessageContaining("coOccurrence must be positive");
    }

    @Test
    void anIncompleteSimilarityPairIsRejected() {
        assertThatThrownBy(() -> new ItemSimilarity(
                " ", "p-2", AffinityScore.of(0.9), 3, COMPUTED_AT))
                .hasMessageContaining("productId");
        assertThatThrownBy(() -> new ItemSimilarity(
                "p-1", null, AffinityScore.of(0.9), 3, COMPUTED_AT))
                .hasMessageContaining("similarProductId");
        assertThatThrownBy(() -> new ItemSimilarity("p-1", "p-2", null, 3, COMPUTED_AT))
                .hasMessageContaining("score must not be null");
    }

    @Test
    void popularityIsUnboundedUnlikeAnAffinityScore() {
        // It is a decayed weight total, not a normalised score - the ranking policy normalises it
        // against the rest of the candidate set instead.
        ProductPopularity popularity = new ProductPopularity(
                "p-1", BigDecimal.valueOf(1_234.5), 900, 40, COMPUTED_AT);

        assertThat(popularity.score()).isEqualByComparingTo(BigDecimal.valueOf(1_234.5));
    }

    @Test
    void aProductWithNoActivityYetIsARepresentableRow() {
        // The popularity job writes a row per product it saw; a zero score is meaningful, a negative one
        // could only come from a broken decay calculation.
        assertThat(new ProductPopularity("p-1", BigDecimal.ZERO, 0, 0, COMPUTED_AT).score().signum())
                .isZero();
        assertThatThrownBy(() -> new ProductPopularity(
                "p-1", BigDecimal.valueOf(-1), 0, 0, COMPUTED_AT))
                .hasMessageContaining("must not be negative");
    }

    @Test
    void negativeCountsAreRejected() {
        assertThatThrownBy(() -> new ProductPopularity("p-1", BigDecimal.ONE, -1, 0, COMPUTED_AT))
                .hasMessageContaining("counts must not be negative");
        assertThatThrownBy(() -> new ProductPopularity("p-1", BigDecimal.ONE, 0, -1, COMPUTED_AT))
                .hasMessageContaining("counts must not be negative");
    }

    @Test
    void anUnattributedPopularityRowIsRejected() {
        assertThatThrownBy(() -> new ProductPopularity(" ", BigDecimal.ONE, 0, 0, COMPUTED_AT))
                .hasMessageContaining("productId must not be blank");
    }
}
