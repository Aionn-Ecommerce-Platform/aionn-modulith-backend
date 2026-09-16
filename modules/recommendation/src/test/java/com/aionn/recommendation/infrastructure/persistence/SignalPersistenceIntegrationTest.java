package com.aionn.recommendation.infrastructure.persistence;

import com.aionn.recommendation.domain.model.ItemSimilarity;
import com.aionn.recommendation.domain.model.ProductPopularity;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.persistence.adapter.ItemSimilarityPersistenceAdapter;
import com.aionn.recommendation.infrastructure.persistence.adapter.PopularityPersistenceAdapter;
import com.aionn.recommendation.infrastructure.persistence.mapper.RecommendationSignalDomainMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the two offline-signal paths that only exist as SQL: the lateral neighbour lookup and the
 * batched JDBC rebuild writes.
 *
 * <p>{@code findNeighboursForAll} binds a {@code String[]} into {@code unnest(CAST(:productIds AS
 * text[]))}, which is the kind of native-query binding Hibernate is free to get wrong, and it sits on
 * the hot path of every signed-in home feed and every similar/also-bought request. A failure there is a
 * 500, not a degraded result, so it is worth a database rather than a mock.
 *
 * <p>Runs on the Flyway schema: the CHECK constraints on score and co-occurrence are part of what the
 * batch writer has to satisfy, and Hibernate-generated DDL would not create them.
 */
@DataJpaTest
@Testcontainers
@EntityScan("com.aionn.recommendation.infrastructure.persistence.entity")
@Import({
        ItemSimilarityPersistenceAdapter.class,
        PopularityPersistenceAdapter.class,
        RecommendationSignalDomainMapper.class,
        SignalPersistenceIntegrationTest.TestConfig.class })
class SignalPersistenceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
    private static final Instant PREVIOUS_RUN = NOW.minusSeconds(3600);

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("recommendation_signal_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db");
    }

    @TestConfiguration
    static class TestConfig {

        /**
         * A batch size of two, so a handful of rows is enough to exercise the chunking loop rather than
         * needing thousands of them.
         */
        @Bean
        RecommendationJobProperties jobProperties() {
            return new RecommendationJobProperties(
                    new RecommendationJobProperties.Profile(10, 180),
                    new RecommendationJobProperties.Similarity(180, 2, 50),
                    new RecommendationJobProperties.Popularity(30),
                    new RecommendationJobProperties.Retention(180),
                    new RecommendationJobProperties.Execution(60, 2));
        }
    }

    @Autowired
    private ItemSimilarityPersistenceAdapter similarityAdapter;
    @Autowired
    private PopularityPersistenceAdapter popularityAdapter;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM recommendation_item_similarity");
        jdbcTemplate.update("DELETE FROM recommendation_popularity");
    }

    @Test
    void neighboursOfSeveralSeedsComeBackInOneQuery() {
        insertSimilarity("p-1", "p-2", 0.9, 8, PREVIOUS_RUN);
        insertSimilarity("p-1", "p-3", 0.4, 3, PREVIOUS_RUN);
        insertSimilarity("p-9", "p-8", 0.7, 5, PREVIOUS_RUN);

        List<ItemSimilarity> neighbours =
                similarityAdapter.findNeighbours(List.of("p-1", "p-9"), 10);

        assertThat(neighbours).hasSize(3);
        assertThat(neighbours).extracting(ItemSimilarity::productId)
                .containsExactlyInAnyOrder("p-1", "p-1", "p-9");
    }

    @Test
    void thePerSeedLimitIsAppliedToEachSeedSeparately() {
        // A global LIMIT with an IN clause would let one popular seed consume the whole budget and leave
        // the other seeds with no neighbours at all, which reads as "nothing is similar to this".
        insertSimilarity("p-hot", "p-a", 0.9, 9, PREVIOUS_RUN);
        insertSimilarity("p-hot", "p-b", 0.8, 8, PREVIOUS_RUN);
        insertSimilarity("p-hot", "p-c", 0.7, 7, PREVIOUS_RUN);
        insertSimilarity("p-quiet", "p-d", 0.3, 2, PREVIOUS_RUN);

        List<ItemSimilarity> neighbours =
                similarityAdapter.findNeighbours(List.of("p-hot", "p-quiet"), 2);

        Map<String, List<ItemSimilarity>> bySeed = neighbours.stream()
                .collect(Collectors.groupingBy(ItemSimilarity::productId));
        assertThat(bySeed.get("p-hot")).hasSize(2);
        assertThat(bySeed.get("p-quiet")).hasSize(1);
    }

    @Test
    void theStrongestNeighboursWinThePerSeedLimit() {
        insertSimilarity("p-1", "p-weak", 0.2, 2, PREVIOUS_RUN);
        insertSimilarity("p-1", "p-strong", 0.95, 20, PREVIOUS_RUN);
        insertSimilarity("p-1", "p-middle", 0.6, 6, PREVIOUS_RUN);

        assertThat(similarityAdapter.findNeighbours(List.of("p-1"), 2))
                .extracting(ItemSimilarity::similarProductId)
                .containsExactly("p-strong", "p-middle");
    }

    @Test
    void aSeedWithNoStoredNeighboursSimplyDoesNotAppear() {
        insertSimilarity("p-1", "p-2", 0.9, 8, PREVIOUS_RUN);

        assertThat(similarityAdapter.findNeighbours(List.of("p-1", "p-unknown"), 10))
                .extracting(ItemSimilarity::productId)
                .containsExactly("p-1");
    }

    @Test
    void anEmptySeedListNeverReachesTheDatabase() {
        assertThat(similarityAdapter.findNeighbours(List.of(), 10)).isEmpty();
        assertThat(similarityAdapter.findNeighbours((List<String>) null, 10)).isEmpty();
    }

    @Test
    void upsertingWritesNewPairsAndReplacesExistingOnes() {
        insertSimilarity("p-1", "p-2", 0.5, 4, PREVIOUS_RUN);

        similarityAdapter.upsertAll(List.of(
                similarity("p-1", "p-2", 0.87, 9),
                similarity("p-3", "p-4", 0.42, 3)));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_item_similarity", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT score FROM recommendation_item_similarity"
                        + " WHERE product_id = 'p-1' AND similar_product_id = 'p-2'",
                BigDecimal.class)).isEqualByComparingTo("0.87");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT co_occurrence FROM recommendation_item_similarity"
                        + " WHERE product_id = 'p-1' AND similar_product_id = 'p-2'",
                Integer.class)).isEqualTo(9);
    }

    @Test
    void rowsAreWrittenAcrossSeveralBatches() {
        // The rebuild commits per batch rather than in one transaction, which is what keeps it inside a
        // transaction budget at all. Splitting the write must not lose or duplicate rows at the boundary.
        List<ItemSimilarity> similarities = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            similarities.add(similarity("p-" + index, "q-" + index, 0.5, 3));
        }

        similarityAdapter.upsertAll(similarities);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_item_similarity", Integer.class)).isEqualTo(5);
    }

    @Test
    void stalePairsAreRemovedByTheTimestampOfTheRunThatWroteThem() {
        insertSimilarity("p-current", "q-current", 0.9, 9, NOW);
        insertSimilarity("p-stale", "q-stale", 0.4, 2, PREVIOUS_RUN);

        int removed = similarityAdapter.deleteComputedBefore(NOW);

        assertThat(removed).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT product_id FROM recommendation_item_similarity", String.class))
                .isEqualTo("p-current");
    }

    @Test
    void popularityUpsertsOnTheProductIdAndKeepsTheHighestRankedFirst() {
        popularityAdapter.upsertAll(List.of(
                popularity("p-quiet", 1.5, 3, 0),
                popularity("p-hot", 120.25, 900, 40)));
        popularityAdapter.upsertAll(List.of(popularity("p-hot", 200.0, 1200, 55)));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_popularity", Integer.class)).isEqualTo(2);
        assertThat(popularityAdapter.findTop(2))
                .extracting(ProductPopularity::productId)
                .containsExactly("p-hot", "p-quiet");
        assertThat(popularityAdapter.findTop(1).getFirst().score())
                .isEqualByComparingTo("200.0");
    }

    @Test
    void stalePopularityRowsAreRemovedSoTheyCannotOutrankActiveProducts() {
        popularityAdapter.upsertAll(List.of(popularity("p-old", 50.0, 100, 10)));
        jdbcTemplate.update(
                "UPDATE recommendation_popularity SET computed_at = ? WHERE product_id = 'p-old'",
                Timestamp.from(PREVIOUS_RUN));

        popularityAdapter.upsertAll(List.of(popularity("p-new", 5.0, 10, 1)));
        int removed = popularityAdapter.deleteComputedBefore(NOW);

        assertThat(removed).isEqualTo(1);
        assertThat(popularityAdapter.findTop(10))
                .extracting(ProductPopularity::productId)
                .containsExactly("p-new");
    }

    private void insertSimilarity(
            String productId, String similarProductId, double score, int coOccurrence, Instant computedAt) {
        jdbcTemplate.update("""
                INSERT INTO recommendation_item_similarity
                    (product_id, similar_product_id, score, co_occurrence, computed_at)
                VALUES (?, ?, ?, ?, ?)
                """, productId, similarProductId, BigDecimal.valueOf(score), coOccurrence,
                Timestamp.from(computedAt));
    }

    private static ItemSimilarity similarity(String productId, String similarProductId,
                                             double score, int coOccurrence) {
        return new ItemSimilarity(
                productId, similarProductId, AffinityScore.of(BigDecimal.valueOf(score)),
                coOccurrence, NOW);
    }

    private static ProductPopularity popularity(String productId, double score, long views, long buys) {
        return new ProductPopularity(productId, BigDecimal.valueOf(score), views, buys, NOW);
    }
}
