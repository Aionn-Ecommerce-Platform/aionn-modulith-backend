package com.aionn.recommendation.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AffinityScoreTest {

    @Test
    void scoreOutsideUnitRangeIsRejected() {
        assertThatThrownBy(() -> AffinityScore.of(BigDecimal.valueOf(1.5)))
                .hasMessageContaining("within [0,1]");
        assertThatThrownBy(() -> AffinityScore.of(BigDecimal.valueOf(-0.1)))
                .hasMessageContaining("within [0,1]");
    }

    @Test
    void normalizeMapsTheRangeOntoZeroToOne() {
        AffinityScore min = AffinityScore.normalize(
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.valueOf(50));
        AffinityScore mid = AffinityScore.normalize(
                BigDecimal.valueOf(30), BigDecimal.TEN, BigDecimal.valueOf(50));
        AffinityScore max = AffinityScore.normalize(
                BigDecimal.valueOf(50), BigDecimal.TEN, BigDecimal.valueOf(50));

        assertThat(min.value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(mid.value()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
        assertThat(max.value()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void aFlatRangeCarriesNoRankingInformation() {
        // Every candidate scored the same, so the signal cannot order them.
        AffinityScore score = AffinityScore.normalize(
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        assertThat(score.isZero()).isTrue();
    }

    @Test
    void valuesOutsideTheObservedRangeAreClamped() {
        AffinityScore above = AffinityScore.normalize(
                BigDecimal.valueOf(99), BigDecimal.TEN, BigDecimal.valueOf(50));
        AffinityScore below = AffinityScore.normalize(
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(50));

        assertThat(above.value()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(below.value()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void nullInputsNormalizeToZeroRatherThanFailing() {
        assertThat(AffinityScore.normalize(null, BigDecimal.ZERO, BigDecimal.ONE).isZero()).isTrue();
    }
}
