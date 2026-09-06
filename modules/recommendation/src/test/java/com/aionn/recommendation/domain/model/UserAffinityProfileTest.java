package com.aionn.recommendation.domain.model;

import com.aionn.recommendation.domain.valueobject.AffinityScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAffinityProfileTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    @Test
    void anEmptyProfileHasNoSignal() {
        assertThat(UserAffinityProfile.empty("user-1").hasNoSignal()).isTrue();
    }

    @Test
    void bestCategoryAffinityTakesTheStrongestMatch() {
        // A product in several categories should be judged by its strongest link to the
        // user, not
        // diluted by its weakest.
        UserAffinityProfile profile = profileWith(
                Map.of("cat-phone", AffinityScore.of(0.9), "cat-cable", AffinityScore.of(0.2)),
                Map.of());

        AffinityScore best = profile.bestCategoryAffinity(List.of("cat-cable", "cat-phone"));

        assertThat(best.value()).isEqualByComparingTo(BigDecimal.valueOf(0.9));
    }

    @Test
    void unknownCategoriesAndBrandsScoreZero() {
        UserAffinityProfile profile = profileWith(
                Map.of("cat-phone", AffinityScore.ONE), Map.of("brand-apple", AffinityScore.ONE));

        assertThat(profile.categoryAffinity("cat-unknown").isZero()).isTrue();
        assertThat(profile.brandAffinity("brand-unknown").isZero()).isTrue();
        assertThat(profile.brandAffinity(null).isZero()).isTrue();
    }

    @Test
    void priceInsideTheBandFitsPerfectly() {
        UserAffinityProfile profile = profileWithBand(
                BigDecimal.valueOf(8_000_000), BigDecimal.valueOf(35_000_000));

        assertThat(profile.priceFit(BigDecimal.valueOf(20_000_000)).value())
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void priceFitTapersAsItLeavesTheBand() {
        UserAffinityProfile profile = profileWithBand(
                BigDecimal.valueOf(10_000_000), BigDecimal.valueOf(20_000_000));

        AffinityScore justOutside = profile.priceFit(BigDecimal.valueOf(22_000_000));
        AffinityScore farOutside = profile.priceFit(BigDecimal.valueOf(28_000_000));

        assertThat(justOutside.value()).isGreaterThan(farOutside.value());
        assertThat(justOutside.isZero()).isFalse();
    }

    @Test
    void aPriceAFullBandWidthAwayDoesNotFitAtAll() {
        UserAffinityProfile profile = profileWithBand(
                BigDecimal.valueOf(10_000_000), BigDecimal.valueOf(20_000_000));

        assertThat(profile.priceFit(BigDecimal.valueOf(30_000_000)).isZero()).isTrue();
    }

    @Test
    void aMissingPriceOrBandScoresNeutralRatherThanPenalising() {
        UserAffinityProfile withBand = profileWithBand(
                BigDecimal.valueOf(10_000_000), BigDecimal.valueOf(20_000_000));
        UserAffinityProfile withoutBand = UserAffinityProfile.empty("user-1");

        assertThat(withBand.priceFit(null).isZero()).isTrue();
        assertThat(withoutBand.priceFit(BigDecimal.valueOf(15_000_000)).isZero()).isTrue();
    }

    @Test
    void anInvertedPriceBandIsRejected() {
        assertThatThrownBy(() -> new UserAffinityProfile(
                "user-1", Map.of(), Map.of(),
                BigDecimal.valueOf(50), BigDecimal.valueOf(10), 1, NOW))
                .hasMessageContaining("priceBandMin must not exceed priceBandMax");
    }

    @Test
    void affinityMapsAreDefensivelyCopied() {
        java.util.Map<String, AffinityScore> mutable = new java.util.HashMap<>();
        mutable.put("cat-phone", AffinityScore.ONE);
        UserAffinityProfile profile = profileWith(mutable, Map.of());

        mutable.clear();

        assertThat(profile.getCategoryAffinities()).containsKey("cat-phone");
    }

    @Test
    void anUnattributedOrImpossibleProfileIsRejected() {
        // A blank user ID would produce a cache entry and a profile row belonging to
        // nobody; a negative
        // count could only come from a broken aggregation.
        assertThatThrownBy(() -> new UserAffinityProfile(
                " ", Map.of(), Map.of(), null, null, 1, NOW))
                .hasMessageContaining("userId must not be blank");
        assertThatThrownBy(() -> new UserAffinityProfile(
                "user-1", Map.of(), Map.of(), null, null, -1, NOW))
                .hasMessageContaining("interactionCount must not be negative");
    }

    @Test
    void aProductWithNoCategoriesAtAllScoresZeroRatherThanFailing() {
        // Catalog permits an unclassified product; it simply cannot match on category.
        UserAffinityProfile profile = profileWith(Map.of("cat-phone", AffinityScore.ONE), Map.of());

        assertThat(profile.bestCategoryAffinity(null).isZero()).isTrue();
        assertThat(profile.bestCategoryAffinity(List.of()).isZero()).isTrue();
    }

    @Test
    void aSingleValuedPriceBandStillProducesATaper() {
        // A user with one observed price has min equal to max, so the band has no
        // width. Falling back to
        // the price itself as the reference keeps the taper meaningful instead of
        // dividing by zero.
        UserAffinityProfile profile = profileWithBand(
                BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(1_000_000));

        assertThat(profile.priceFit(BigDecimal.valueOf(1_000_000)).value())
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(profile.priceFit(BigDecimal.valueOf(1_500_000)).value())
                .isBetween(BigDecimal.ZERO, BigDecimal.ONE);
        assertThat(profile.priceFit(BigDecimal.valueOf(9_000_000)).isZero()).isTrue();
    }

    @Test
    void theLastInteractionIsAbsentUntilThereIsOne() {
        assertThat(UserAffinityProfile.empty("user-1").lastInteraction()).isEmpty();
        assertThat(profileWith(Map.of(), Map.of()).lastInteraction()).contains(NOW);
    }

    private static UserAffinityProfile profileWith(
            Map<String, AffinityScore> categories, Map<String, AffinityScore> brands) {
        return new UserAffinityProfile("user-1", categories, brands, null, null, 10, NOW);
    }

    private static UserAffinityProfile profileWithBand(BigDecimal min, BigDecimal max) {
        return new UserAffinityProfile("user-1", Map.of(), Map.of(), min, max, 10, NOW);
    }
}
