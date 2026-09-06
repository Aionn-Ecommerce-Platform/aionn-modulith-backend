package com.aionn.recommendation.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InteractionWeightTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    @Test
    void freshInteractionKeepsItsFullWeight() {
        InteractionWeight weight = InteractionWeight.of(BigDecimal.valueOf(5), Duration.ofDays(180));

        assertThat(weight.decayedAt(NOW, NOW)).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void oneHalfLifeHalvesTheWeight() {
        InteractionWeight weight = InteractionWeight.of(BigDecimal.valueOf(4), Duration.ofDays(30));

        BigDecimal decayed = weight.decayedAt(NOW.minus(Duration.ofDays(30)), NOW);

        assertThat(decayed).isCloseTo(BigDecimal.valueOf(2), org.assertj.core.data.Offset.offset(
                BigDecimal.valueOf(0.0001)));
    }

    @Test
    void twoHalfLivesQuarterTheWeight() {
        InteractionWeight weight = InteractionWeight.of(BigDecimal.valueOf(4), Duration.ofDays(14));

        BigDecimal decayed = weight.decayedAt(NOW.minus(Duration.ofDays(28)), NOW);

        assertThat(decayed).isCloseTo(BigDecimal.ONE, org.assertj.core.data.Offset.offset(
                BigDecimal.valueOf(0.0001)));
    }

    @Test
    void aShorterHalfLifeDecaysFasterOverTheSameAge() {
        Instant fortnightAgo = NOW.minus(Duration.ofDays(14));
        BigDecimal viewLike = InteractionWeight.of(BigDecimal.ONE, Duration.ofDays(14))
                .decayedAt(fortnightAgo, NOW);
        BigDecimal purchaseLike = InteractionWeight.of(BigDecimal.ONE, Duration.ofDays(180))
                .decayedAt(fortnightAgo, NOW);

        assertThat(viewLike).isLessThan(purchaseLike);
    }

    @Test
    void futureDatedInteractionIsNotAmplified() {
        // Clock skew between instances can date an interaction ahead of now; a negative
        // age would
        // otherwise raise the weight above its base.
        InteractionWeight weight = InteractionWeight.of(BigDecimal.valueOf(3), Duration.ofDays(30));

        BigDecimal decayed = weight.decayedAt(NOW.plus(Duration.ofHours(1)), NOW);

        assertThat(decayed).isEqualByComparingTo(BigDecimal.valueOf(3));
    }

    @Test
    void nonPositiveBaseWeightIsRejected() {
        assertThatThrownBy(() -> InteractionWeight.of(BigDecimal.ZERO, Duration.ofDays(1)))
                .hasMessageContaining("baseWeight must be positive");
    }

    @Test
    void subSecondHalfLifeIsPreserved() {
        InteractionWeight weight = InteractionWeight.of(BigDecimal.valueOf(2), Duration.ofMillis(500));
        BigDecimal decayed = weight.decayedAt(NOW.minus(Duration.ofMillis(500)), NOW);

        assertThat(decayed).isCloseTo(BigDecimal.ONE, org.assertj.core.data.Offset.offset(
                BigDecimal.valueOf(0.0001)));
    }

    @Test
    void nonPositiveHalfLifeIsRejected() {
        assertThatThrownBy(() -> InteractionWeight.of(BigDecimal.ONE, Duration.ZERO))
                .hasMessageContaining("halfLife must be positive");
    }
}
