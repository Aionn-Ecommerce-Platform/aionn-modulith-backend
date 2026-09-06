package com.aionn.recommendation.infrastructure.config;

import com.aionn.recommendation.application.policy.ColdStartThresholdPolicy;
import com.aionn.recommendation.application.policy.InteractionWeightPolicy;
import com.aionn.recommendation.application.policy.RankingWeightPolicy;
import com.aionn.recommendation.domain.valueobject.InteractionWeight;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationColdStartProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationRankingProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationWeightProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Adapts configuration properties into the application-facing policies.
 * Application code must not
 * import properties classes, so the translation happens here.
 */
@Configuration
public class RecommendationPolicyConfig {

    @Bean
    public InteractionWeightPolicy interactionWeightPolicy(RecommendationWeightProperties properties) {
        return type -> switch (type) {
            case VIEW -> toWeight(properties.view());
            case CART_ADD -> toWeight(properties.cartAdd());
            case PURCHASE -> toWeight(properties.purchase());
        };
    }

    @Bean
    public RankingWeightPolicy rankingWeightPolicy(RecommendationRankingProperties properties) {
        return new PropertyBackedRankingWeightPolicy(properties);
    }

    @Bean
    public ColdStartThresholdPolicy coldStartThresholdPolicy(
            RecommendationColdStartProperties properties) {
        return new PropertyBackedColdStartThresholdPolicy(properties);
    }

    private static InteractionWeight toWeight(RecommendationWeightProperties.Signal signal) {
        return InteractionWeight.of(signal.base(), Duration.ofDays(signal.halfLifeDays()));
    }

    private record PropertyBackedRankingWeightPolicy(RecommendationRankingProperties properties)
            implements RankingWeightPolicy {

        @Override
        public BigDecimal collaborativeWeight() {
            return properties.collaborativeWeight();
        }

        @Override
        public BigDecimal contentWeight() {
            return properties.contentWeight();
        }

        @Override
        public BigDecimal popularityWeight() {
            return properties.popularityWeight();
        }

        @Override
        public BigDecimal categoryAffinityWeight() {
            return properties.categoryAffinityWeight();
        }

        @Override
        public BigDecimal brandAffinityWeight() {
            return properties.brandAffinityWeight();
        }

        @Override
        public BigDecimal priceFitWeight() {
            return properties.priceFitWeight();
        }

        @Override
        public int candidateOverFetchFactor() {
            return Math.max(1, properties.candidateOverFetchFactor());
        }

        @Override
        public int maxCandidates() {
            return Math.max(1, properties.maxCandidates());
        }
    }

    private record PropertyBackedColdStartThresholdPolicy(
            RecommendationColdStartProperties properties) implements ColdStartThresholdPolicy {

        @Override
        public int contentOnlyThreshold() {
            return Math.max(1, properties.contentOnlyThreshold());
        }

        @Override
        public int fullHybridThreshold() {
            return Math.max(contentOnlyThreshold(), properties.fullHybridThreshold());
        }
    }
}
