package com.aionn.recommendation.infrastructure.config.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the configuration surface the module exposes to operators.
 *
 * <p>Every value is bound through the shipped {@code application-recommendation.yml} rather than invented
 * in the test, so the assertions are about the configuration the application actually starts with. The
 * failure mode being prevented is not a typo in a test: it is an application that starts healthy, reports
 * UP, and then silently discards behavioural signals because a weight or a window was set to something
 * the domain cannot represent.
 *
 * <p>Overrides are supplied as the environment variables an operator would set, not as the bound property
 * paths. The YAML expresses every value as {@code ${ENV_VAR:default}}, so this exercises the real
 * override path and keeps each test independent of property-source ordering. It also means a knob that
 * lost its wiring between the env file and the YAML fails here rather than in production.
 *
 * <p>No validator bean is registered on purpose: {@code ConfigurationPropertiesBinder} builds its own
 * JSR-303 validator when {@code @Validated} is present and an implementation is on the classpath, which
 * is the mechanism the application relies on. Registering one here would let the test pass on a validator
 * the application never uses.
 */
class RecommendationPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesUnderTest.class)
            .withInitializer(shippedYaml());

    @Test
    void theShippedConfigurationStartsAndCarriesTheDocumentedValues() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();

            // The values below are the ones published in .env.example and envs/recommendation.env. A
            // silent divergence between the YAML and the operator-facing list is what this pins: the env
            // file is how an operator learns what the knobs are, so it must not drift.
            RecommendationWeightProperties weights =
                    context.getBean(RecommendationWeightProperties.class);
            assertThat(weights.view().base()).isEqualByComparingTo("1.0");
            assertThat(weights.view().halfLifeDays()).isEqualTo(14);
            assertThat(weights.cartAdd().base()).isEqualByComparingTo("4.0");
            assertThat(weights.cartAdd().halfLifeDays()).isEqualTo(30);
            assertThat(weights.purchase().base()).isEqualByComparingTo("5.0");
            assertThat(weights.purchase().halfLifeDays()).isEqualTo(180);

            RecommendationRankingProperties ranking =
                    context.getBean(RecommendationRankingProperties.class);
            assertThat(ranking.collaborativeWeight()).isEqualByComparingTo("0.50");
            assertThat(ranking.contentWeight()).isEqualByComparingTo("0.35");
            assertThat(ranking.popularityWeight()).isEqualByComparingTo("0.15");
            assertThat(ranking.categoryAffinityWeight()).isEqualByComparingTo("0.45");
            assertThat(ranking.brandAffinityWeight()).isEqualByComparingTo("0.35");
            assertThat(ranking.priceFitWeight()).isEqualByComparingTo("0.20");
            assertThat(ranking.candidateOverFetchFactor()).isEqualTo(3);
            assertThat(ranking.maxCandidates()).isEqualTo(200);

            RecommendationColdStartProperties coldStart =
                    context.getBean(RecommendationColdStartProperties.class);
            assertThat(coldStart.contentOnlyThreshold()).isEqualTo(1);
            assertThat(coldStart.fullHybridThreshold()).isEqualTo(5);

            RecommendationJobProperties jobs = context.getBean(RecommendationJobProperties.class);
            assertThat(jobs.profile().maxAffinities()).isEqualTo(10);
            assertThat(jobs.profile().lookbackDays()).isEqualTo(180);
            assertThat(jobs.similarity().lookbackDays()).isEqualTo(180);
            assertThat(jobs.similarity().minCoOccurrence()).isEqualTo(2);
            assertThat(jobs.similarity().maxNeighboursPerProduct()).isEqualTo(50);
            assertThat(jobs.popularity().lookbackDays()).isEqualTo(30);
            assertThat(jobs.retention().interactionMaxAgeDays()).isEqualTo(180);
            assertThat(jobs.execution().computeTimeoutSeconds()).isEqualTo(900);
            assertThat(jobs.execution().upsertBatchSize()).isEqualTo(500);

            RecommendationSchedulingProperties scheduling =
                    context.getBean(RecommendationSchedulingProperties.class);
            assertThat(scheduling.profileRefresh().enabled()).isTrue();
            assertThat(scheduling.profileRefresh().delayMs()).isEqualTo(900_000L);
            assertThat(scheduling.profileRefresh().batchSize()).isEqualTo(500);
            assertThat(scheduling.itemSimilarity().delayMs()).isEqualTo(3_600_000L);
            assertThat(scheduling.popularity().delayMs()).isEqualTo(900_000L);
            assertThat(scheduling.prune().delayMs()).isEqualTo(86_400_000L);
            assertThat(scheduling.prune().batchSize()).isEqualTo(1000);

            RecommendationCacheProperties cache = context.getBean(RecommendationCacheProperties.class);
            assertThat(cache.home().l1TtlSeconds()).isEqualTo(60);
            assertThat(cache.home().l2TtlSeconds()).isEqualTo(900);
            assertThat(cache.similar().l1TtlSeconds()).isEqualTo(300);
            assertThat(cache.similar().l2TtlSeconds()).isEqualTo(3600);
            assertThat(cache.trending().l1MaxSize()).isEqualTo(10);
        });
    }

    /**
     * Each of these is a value an operator could plausibly type, and each one used to be accepted.
     *
     * <p>The weight cases are the expensive ones. A zero half-life makes {@code InteractionWeight} throw
     * on every ingest, and a base above 999.99 overflows the {@code NUMERIC(5,2)} column that stores it.
     * Neither shows up at startup on its own - the application comes up, reports healthy, and then loses
     * the signal, which is indistinguishable from nobody using the product.
     */
    @ParameterizedTest(name = "{0} must stop the application from starting")
    @ValueSource(strings = {
            "RECOMMENDATION_WEIGHT_VIEW_HALF_LIFE_DAYS=0",
            "RECOMMENDATION_WEIGHT_VIEW_BASE=10000",
            "RECOMMENDATION_WEIGHT_PURCHASE_BASE=0",
            "RECOMMENDATION_RANKING_COLLABORATIVE_WEIGHT=-0.5",
            "RECOMMENDATION_RANKING_MAX_CANDIDATES=0",
            "RECOMMENDATION_RANKING_CANDIDATE_OVER_FETCH_FACTOR=0",
            "RECOMMENDATION_COLD_START_CONTENT_ONLY_THRESHOLD=0",
            "RECOMMENDATION_PROFILE_LOOKBACK_DAYS=0",
            "RECOMMENDATION_SIMILARITY_MIN_CO_OCCURRENCE=0",
            "RECOMMENDATION_POPULARITY_LOOKBACK_DAYS=0",
            "RECOMMENDATION_RETENTION_INTERACTION_MAX_AGE_DAYS=0",
            "RECOMMENDATION_EXECUTION_COMPUTE_TIMEOUT_SECONDS=0",
            "RECOMMENDATION_EXECUTION_UPSERT_BATCH_SIZE=0",
            "RECOMMENDATION_SCHEDULING_PROFILE_REFRESH_DELAY_MS=10",
            "RECOMMENDATION_SCHEDULING_PRUNE_BATCH_SIZE=0",
            "RECOMMENDATION_CACHE_HOME_L1_TTL_SECONDS=0",
            "RECOMMENDATION_CACHE_TRENDING_L2_TTL_SECONDS=0"})
    void anOutOfRangeValueStopsTheApplicationFromStarting(String override) {
        runner.withPropertyValues(override).run(context -> {
            assertThat(context).hasFailed();
            // The failure has to arrive as a validation failure. A binding error, a
            // NumberFormatException, or a context that starts anyway would all mean the constraint is
            // not what stopped it, and the message an operator sees would not name the value they got
            // wrong.
            assertThat(context).getFailure()
                    .rootCause()
                    .isInstanceOf(BindValidationException.class);
        });
    }

    /**
     * Loads the module's own {@code application-recommendation.yml}.
     *
     * <p>Added last, so it behaves like configuration data in the real application: the lowest-precedence
     * source, overridden by anything the environment supplies.
     */
    private static ApplicationContextInitializer<ConfigurableApplicationContext> shippedYaml() {
        return context -> {
            try {
                List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(
                        "application-recommendation.yml",
                        new ClassPathResource("application-recommendation.yml"));
                assertThat(sources)
                        .as("the module's configuration file must be on the test classpath")
                        .isNotEmpty();
                sources.forEach(source ->
                        context.getEnvironment().getPropertySources().addLast(source));
            } catch (IOException exception) {
                throw new UncheckedIOException(
                        "could not load application-recommendation.yml for the binding test", exception);
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            RecommendationCacheProperties.class,
            RecommendationColdStartProperties.class,
            RecommendationJobProperties.class,
            RecommendationRankingProperties.class,
            RecommendationSchedulingProperties.class,
            RecommendationWeightProperties.class})
    static class PropertiesUnderTest {
    }
}
