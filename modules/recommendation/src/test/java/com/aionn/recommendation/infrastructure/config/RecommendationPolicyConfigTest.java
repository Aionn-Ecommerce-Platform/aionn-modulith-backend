package com.aionn.recommendation.infrastructure.config;

import com.aionn.recommendation.application.policy.ColdStartThresholdPolicy;
import com.aionn.recommendation.application.policy.InteractionWeightPolicy;
import com.aionn.recommendation.application.policy.RankingWeightPolicy;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationColdStartProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationRankingProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationWeightProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the translation from configuration properties to the policies application code consumes.
 *
 * <p>Application services are not allowed to import properties classes, so this config class is the only
 * place the two meet. That makes it the place where a configuration value can be silently dropped or
 * misread, and it is also where one deliberate startup failure lives: a set of ranking weights that sum
 * to zero produces an empty slate on every surface, which a reader cannot distinguish from an empty
 * catalogue.
 */
class RecommendationPolicyConfigTest {

    private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PoliciesUnderTest.class, RecommendationPolicyConfig.class)
            .withInitializer(shippedYaml());

    @Test
    void everyConfiguredRankingWeightReachesThePolicy() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();

            RankingWeightPolicy policy = context.getBean(RankingWeightPolicy.class);
            assertThat(policy.collaborativeWeight()).isEqualByComparingTo("0.50");
            assertThat(policy.contentWeight()).isEqualByComparingTo("0.35");
            assertThat(policy.popularityWeight()).isEqualByComparingTo("0.15");
            assertThat(policy.categoryAffinityWeight()).isEqualByComparingTo("0.45");
            assertThat(policy.brandAffinityWeight()).isEqualByComparingTo("0.35");
            assertThat(policy.priceFitWeight()).isEqualByComparingTo("0.20");
            assertThat(policy.candidateOverFetchFactor()).isEqualTo(3);
            assertThat(policy.maxCandidates()).isEqualTo(200);
        });
    }

    /**
     * Each signal must carry its own base and its own half-life. Reading the wrong nested record here
     * would still produce plausible numbers - a view weighted like a purchase - so the assertion checks
     * the decay at exactly one half-life per type, which is only right if both fields came from the same
     * signal.
     */
    @Test
    void eachSignalTypeGetsItsOwnBaseAndHalfLife() {
        runner.run(context -> {
            InteractionWeightPolicy policy = context.getBean(InteractionWeightPolicy.class);

            assertDecays(policy, InteractionType.VIEW, "1.0", Duration.ofDays(14));
            assertDecays(policy, InteractionType.CART_ADD, "4.0", Duration.ofDays(30));
            assertDecays(policy, InteractionType.PURCHASE, "5.0", Duration.ofDays(180));
        });
    }

    /**
     * Zero is accepted by the per-field constraints - each weight is legitimately allowed to be switched
     * off - so the combination has to be rejected where it is understood. All three at zero leaves the
     * hybrid policy dividing by zero, and every surface then returns an empty slate with no error
     * anywhere for an operator to find.
     */
    @Test
    void rankingWeightsThatSumToZeroStopTheApplicationFromStarting() {
        runner.withPropertyValues(
                        "RECOMMENDATION_RANKING_COLLABORATIVE_WEIGHT=0",
                        "RECOMMENDATION_RANKING_CONTENT_WEIGHT=0",
                        "RECOMMENDATION_RANKING_POPULARITY_WEIGHT=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context).getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("must sum to a positive value");
                });
    }

    /**
     * An inverted threshold pair is a misconfiguration the policy absorbs rather than rejects, because
     * there is a safe reading of it: treat both thresholds as the larger one, so the user stays in
     * content-only until they have the history full hybrid needs. Failing to start would take the whole
     * recommendation surface down over a pair of numbers that still has a sensible interpretation.
     */
    @Test
    void anInvertedColdStartPairIsClampedRatherThanRejected() {
        ColdStartThresholdPolicy policy = new RecommendationPolicyConfig()
                .coldStartThresholdPolicy(new RecommendationColdStartProperties(10, 3));

        assertThat(policy.contentOnlyThreshold()).isEqualTo(10);
        assertThat(policy.fullHybridThreshold()).isEqualTo(10);
    }

    private static void assertDecays(InteractionWeightPolicy policy, InteractionType type,
            String expectedBase, Duration halfLife) {
        BigDecimal fresh = policy.weightFor(type).decayedAt(NOW, NOW);
        assertThat(fresh)
                .as("%s weight at zero age", type)
                .isEqualByComparingTo(expectedBase);

        BigDecimal aged = policy.weightFor(type).decayedAt(NOW.minus(halfLife), NOW);
        assertThat(aged)
                .as("%s weight after exactly one half-life", type)
                .isEqualByComparingTo(new BigDecimal(expectedBase).divide(BigDecimal.valueOf(2)));
    }

    /**
     * Loads the module's own {@code application-recommendation.yml} as the lowest-precedence source, the
     * way configuration data behaves in the real application, so the environment overrides the tests
     * supply always win.
     */
    private static ApplicationContextInitializer<ConfigurableApplicationContext> shippedYaml() {
        return context -> {
            try {
                List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(
                        "application-recommendation.yml",
                        new ClassPathResource("application-recommendation.yml"));
                sources.forEach(source ->
                        context.getEnvironment().getPropertySources().addLast(source));
            } catch (IOException exception) {
                throw new UncheckedIOException(
                        "could not load application-recommendation.yml for the policy test", exception);
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            RecommendationColdStartProperties.class,
            RecommendationRankingProperties.class,
            RecommendationWeightProperties.class})
    static class PoliciesUnderTest {
    }
}
