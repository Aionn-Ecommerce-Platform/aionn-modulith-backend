package com.aionn.recommendation.domain.model;

import com.aionn.recommendation.domain.valueobject.InteractionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserInteractionTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-09-05T12:00:00Z");

    @Test
    void anInteractionIsStoredWithItsBaseWeightAndTheMomentItHappened() {
        // Not the moment it was ingested: decay is measured from the user's action, and
        // a replayed
        // backlog must not look like fresh interest.
        UserInteraction interaction = UserInteraction.create(
                "int-1", "user-1", "p-1", InteractionType.VIEW, BigDecimal.ONE, OCCURRED_AT);

        assertThat(interaction.getWeight()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(interaction.getOccurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void onlyDeliberateIntentCountsAsAStrongSignal() {
        // Collaborative filtering runs on strong signals alone; users browse widely
        // without meaning to
        // buy, so treating views as intent would relate everything to everything.
        assertThat(interactionOf(InteractionType.VIEW).isStrongSignal()).isFalse();
        assertThat(interactionOf(InteractionType.CART_ADD).isStrongSignal()).isTrue();
        assertThat(interactionOf(InteractionType.PURCHASE).isStrongSignal()).isTrue();
    }

    @Test
    void aZeroOrNegativeWeightIsRejected() {
        // A zero-weight row would survive every decay calculation while contributing
        // nothing, and the
        // table's CHECK constraint would reject it at the boundary anyway.
        assertThatThrownBy(() -> UserInteraction.create(
                "int-1", "user-1", "p-1", InteractionType.VIEW, BigDecimal.ZERO, OCCURRED_AT))
                .hasMessageContaining("weight must be positive");
        assertThatThrownBy(() -> UserInteraction.create(
                "int-1", "user-1", "p-1", InteractionType.VIEW, BigDecimal.valueOf(-1), OCCURRED_AT))
                .hasMessageContaining("weight must be positive");
    }

    @Test
    void anInteractionWithoutASubjectOrAProductIsRejected() {
        assertThatThrownBy(() -> UserInteraction.create(
                "int-1", " ", "p-1", InteractionType.VIEW, BigDecimal.ONE, OCCURRED_AT))
                .hasMessageContaining("userId");
        assertThatThrownBy(() -> UserInteraction.create(
                "int-1", "user-1", null, InteractionType.VIEW, BigDecimal.ONE, OCCURRED_AT))
                .hasMessageContaining("productId");
    }

    @Test
    void anUntypedOrUndatedInteractionIsRejected() {
        assertThatThrownBy(() -> UserInteraction.create(
                "int-1", "user-1", "p-1", null, BigDecimal.ONE, OCCURRED_AT))
                .hasMessageContaining("interaction type");
        assertThatThrownBy(() -> UserInteraction.create(
                "int-1", "user-1", "p-1", InteractionType.VIEW, BigDecimal.ONE, null))
                .hasMessageContaining("occurredAt");
    }

    private static UserInteraction interactionOf(InteractionType type) {
        return UserInteraction.create("int-1", "user-1", "p-1", type, BigDecimal.ONE, OCCURRED_AT);
    }
}
