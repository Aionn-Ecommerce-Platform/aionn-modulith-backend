package com.aionn.recommendation.application.policy;

import com.aionn.recommendation.domain.model.UserAffinityProfile;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ColdStartPolicyTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    private final ColdStartPolicy policy = new ColdStartPolicy(
            thresholds(1, 5), weights());

    @Test
    void aUserWithNoHistoryGetsPopularityOnly() {
        ColdStartPolicy.SignalWeights resolved =
                policy.resolve(UserAffinityProfile.empty("user-1"));

        assertThat(resolved.isPopularityOnly()).isTrue();
    }

    @Test
    void aNullProfileIsTreatedAsNoHistory() {
        assertThat(policy.resolve(null).isPopularityOnly()).isTrue();
    }

    @Test
    void aSparseUserGetsContentButNotCollaborative() {
        // Two interactions are enough to infer category and brand taste, but a cosine over two data
        // points is noise rather than signal.
        ColdStartPolicy.SignalWeights resolved = policy.resolve(profileWith(2));

        assertThat(resolved.usesCollaborative()).isFalse();
        assertThat(resolved.usesContent()).isTrue();
    }

    @Test
    void anEstablishedUserGetsTheFullHybrid() {
        ColdStartPolicy.SignalWeights resolved = policy.resolve(profileWith(20));

        assertThat(resolved.usesCollaborative()).isTrue();
        assertThat(resolved.usesContent()).isTrue();
    }

    @Test
    void aProfileWithInteractionsButNoUsableSignalFallsBackToPopularity() {
        // Interactions counted, but every product they touched has since been unpublished, so no
        // affinity survived the refresh.
        UserAffinityProfile noSignal = new UserAffinityProfile(
                "user-1", Map.of(), Map.of(), null, null, 50, NOW);

        assertThat(policy.resolve(noSignal).isPopularityOnly()).isTrue();
    }

    @Test
    void productSurfacesRelyOnSimilarityRatherThanAUserProfile() {
        ColdStartPolicy.SignalWeights resolved = policy.forProductSurface();

        assertThat(resolved.usesCollaborative()).isTrue();
        assertThat(resolved.usesContent()).isFalse();
    }

    @Test
    void thresholdsAreOrderedEvenIfMisconfigured() {
        // A full-hybrid threshold below the content threshold would leave a gap where neither applies.
        ColdStartPolicy misordered = new ColdStartPolicy(thresholds(5, 2), weights());

        assertThat(misordered.resolve(profileWith(3)).usesContent()).isFalse();
    }

    private static UserAffinityProfile profileWith(int interactionCount) {
        return new UserAffinityProfile(
                "user-1",
                Map.of("cat-phone", AffinityScore.ONE),
                Map.of("brand-apple", AffinityScore.ONE),
                null,
                null,
                interactionCount,
                NOW);
    }

    private static ColdStartThresholdPolicy thresholds(int contentOnly, int fullHybrid) {
        return new ColdStartThresholdPolicy() {
            @Override
            public int contentOnlyThreshold() {
                return contentOnly;
            }

            @Override
            public int fullHybridThreshold() {
                return Math.max(contentOnly, fullHybrid);
            }
        };
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
