package com.aionn.recommendation.application.policy;

import com.aionn.recommendation.domain.model.UserAffinityProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Decides how much weight each signal carries for a given user.
 *
 * <p>Cold start is treated as a continuous adjustment rather than an error path. A user with two
 * interactions has enough to infer category and brand taste but nothing meaningful for collaborative
 * filtering - a cosine over two data points is noise. So the collaborative weight is redistributed
 * instead of the whole request falling back to a generic list.
 */
@Component
@RequiredArgsConstructor
public class ColdStartPolicy {

    private final ColdStartThresholdPolicy thresholds;
    private final RankingWeightPolicy weights;

    public SignalWeights resolve(UserAffinityProfile profile) {
        if (profile == null || profile.getInteractionCount() < thresholds.contentOnlyThreshold()
                || profile.hasNoSignal()) {
            return SignalWeights.popularityOnly();
        }
        if (profile.getInteractionCount() < thresholds.fullHybridThreshold()) {
            return SignalWeights.withoutCollaborative(
                    weights.contentWeight(), weights.popularityWeight());
        }
        return new SignalWeights(
                weights.collaborativeWeight(), weights.contentWeight(), weights.popularityWeight());
    }

    /** Product-scoped surfaces have no user profile, so similarity and content carry the slate. */
    public SignalWeights forProductSurface() {
        return SignalWeights.withoutContentProfile(
                weights.collaborativeWeight(), weights.popularityWeight());
    }

    /**
     * Normalised weights for the three candidate sources. Values are relative; the ranking policy
     * divides by their sum, so they need not add to one.
     */
    public record SignalWeights(
            BigDecimal collaborative, BigDecimal content, BigDecimal popularity) {

        public static SignalWeights popularityOnly() {
            return new SignalWeights(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE);
        }

        /**
         * Cold-start slate that blends recent momentum with new arrivals. New arrivals ride the
         * content slot because they are a catalog-derived signal, not a behavioural one.
         */
        public static SignalWeights trendingWithNewArrivals() {
            return new SignalWeights(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE);
        }

        static SignalWeights withoutCollaborative(BigDecimal content, BigDecimal popularity) {
            return new SignalWeights(BigDecimal.ZERO, content, popularity);
        }

        static SignalWeights withoutContentProfile(BigDecimal collaborative, BigDecimal popularity) {
            return new SignalWeights(collaborative, BigDecimal.ZERO, popularity);
        }

        public boolean usesCollaborative() {
            return collaborative.signum() > 0;
        }

        public boolean usesContent() {
            return content.signum() > 0;
        }

        public boolean isPopularityOnly() {
            return !usesCollaborative() && !usesContent();
        }
    }
}
