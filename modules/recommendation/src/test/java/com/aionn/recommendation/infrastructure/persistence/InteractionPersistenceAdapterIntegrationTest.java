package com.aionn.recommendation.infrastructure.persistence;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.domain.model.UserInteraction;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.recommendation.infrastructure.persistence.adapter.InteractionPersistenceAdapter;
import com.aionn.recommendation.infrastructure.persistence.entity.InteractionEntity;
import com.aionn.recommendation.infrastructure.persistence.mapper.InteractionDomainMapper;
import com.aionn.recommendation.infrastructure.persistence.repository.InteractionRepository;
import com.aionn.sharedkernel.util.IdGenerator;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the SQL that carries real semantics: exponential decay and the
 * item-item cosine. Both live in
 * native queries, so unit tests over a mocked repository would prove nothing
 * about them.
 */
@DataJpaTest
@Testcontainers
@EntityScan("com.aionn.recommendation.infrastructure.persistence.entity")
@Import({
        InteractionPersistenceAdapter.class,
        InteractionDomainMapper.class,
        InteractionPersistenceAdapterIntegrationTest.FixedClockConfig.class })
class InteractionPersistenceAdapterIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("recommendation_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private InteractionRepository repository;
    @Autowired
    private InteractionPersistenceAdapter adapter;

    @BeforeEach
    void resetData() {
        repository.deleteAll();
    }

    @Test
    void aRecentInteractionOutweighsAnOlderOneOfTheSameKind() {
        // The reason base weights are stored and decay is applied at read time.
        persist("user-1", "p-recent", InteractionType.PURCHASE, 5, NOW.minus(Duration.ofDays(1)));
        persist("user-2", "p-stale", InteractionType.PURCHASE, 5, NOW.minus(Duration.ofDays(150)));

        Map<String, InteractionPersistencePort.PopularityAggregate> aggregates = adapter
                .aggregatePopularity(NOW.minus(Duration.ofDays(365)), NOW, halfLives());

        assertThat(aggregates.get("p-recent").decayedScore())
                .isGreaterThan(aggregates.get("p-stale").decayedScore());
    }

    @Test
    void oneHalfLifeHalvesTheContributionInSql() {
        persist("user-1", "p-fresh", InteractionType.CART_ADD, 4, NOW);
        persist("user-2", "p-aged", InteractionType.CART_ADD, 4, NOW.minus(Duration.ofDays(30)));

        Map<String, InteractionPersistencePort.PopularityAggregate> aggregates = adapter
                .aggregatePopularity(NOW.minus(Duration.ofDays(365)), NOW, halfLives());

        double fresh = aggregates.get("p-fresh").decayedScore().doubleValue();
        double aged = aggregates.get("p-aged").decayedScore().doubleValue();
        assertThat(aged).isCloseTo(fresh / 2, Percentage.withPercentage(1));
    }

    @Test
    void viewsAndPurchasesAreCountedSeparately() {
        persist("user-1", "p-1", InteractionType.VIEW, 1, NOW);
        persist("user-2", "p-1", InteractionType.VIEW, 1, NOW);
        persist("user-1", "p-1", InteractionType.PURCHASE, 5, NOW);

        InteractionPersistencePort.PopularityAggregate aggregate = adapter
                .aggregatePopularity(NOW.minus(Duration.ofDays(30)), NOW, halfLives()).get("p-1");

        assertThat(aggregate.viewCount()).isEqualTo(2);
        assertThat(aggregate.purchaseCount()).isEqualTo(1);
    }

    @Test
    void interactionsOutsideTheWindowAreExcluded() {
        persist("user-1", "p-old", InteractionType.VIEW, 1, NOW.minus(Duration.ofDays(60)));

        assertThat(adapter.aggregatePopularity(NOW.minus(Duration.ofDays(30)), NOW, halfLives()))
                .isEmpty();
    }

    @Test
    void cosineSimilarityMatchesTheClosedFormForASharedBuyer() {
        // Two users bought p-1, one of whom also bought p-2: 1 / sqrt(2 * 1) = 0.7071.
        persist("user-1", "p-1", InteractionType.PURCHASE, 5, NOW);
        persist("user-2", "p-1", InteractionType.PURCHASE, 5, NOW);
        persist("user-1", "p-2", InteractionType.PURCHASE, 5, NOW);

        List<InteractionPersistencePort.SimilarityRow> rows = adapter.computeItemSimilarity(
                NOW.minus(Duration.ofDays(180)), strongTypes(), 1);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().score()).isCloseTo(0.7071, Offset.offset(0.0001));
        assertThat(rows.getFirst().coOccurrence()).isEqualTo(1);
    }

    @Test
    void eachUnorderedPairIsReturnedOnlyOnce() {
        persist("user-1", "p-1", InteractionType.PURCHASE, 5, NOW);
        persist("user-1", "p-2", InteractionType.PURCHASE, 5, NOW);
        persist("user-2", "p-1", InteractionType.PURCHASE, 5, NOW);
        persist("user-2", "p-2", InteractionType.PURCHASE, 5, NOW);

        assertThat(adapter.computeItemSimilarity(
                NOW.minus(Duration.ofDays(180)), strongTypes(), 1)).hasSize(1);
    }

    @Test
    void pairsBelowTheCoOccurrenceFloorAreDiscarded() {
        // One shared buyer yields a high cosine that means nothing; the floor keeps it
        // out.
        persist("user-1", "p-1", InteractionType.PURCHASE, 5, NOW);
        persist("user-1", "p-2", InteractionType.PURCHASE, 5, NOW);

        assertThat(adapter.computeItemSimilarity(
                NOW.minus(Duration.ofDays(180)), strongTypes(), 2)).isEmpty();
    }

    @Test
    void viewsDoNotContributeToSimilarity() {
        persist("user-1", "p-1", InteractionType.VIEW, 1, NOW);
        persist("user-1", "p-2", InteractionType.VIEW, 1, NOW);
        persist("user-2", "p-1", InteractionType.VIEW, 1, NOW);
        persist("user-2", "p-2", InteractionType.VIEW, 1, NOW);

        assertThat(adapter.computeItemSimilarity(
                NOW.minus(Duration.ofDays(180)), strongTypes(), 1)).isEmpty();
    }

    @Test
    void repeatedPurchasesByTheSameUserCountAsOneCoOccurrence() {
        // DISTINCT on (user, product) stops one repeat buyer from inflating a pair.
        persist("user-1", "p-1", InteractionType.PURCHASE, 5, NOW);
        persist("user-1", "p-1", InteractionType.PURCHASE, 5, NOW.minus(Duration.ofDays(2)));
        persist("user-1", "p-2", InteractionType.PURCHASE, 5, NOW);
        persist("user-2", "p-1", InteractionType.PURCHASE, 5, NOW);
        persist("user-2", "p-2", InteractionType.PURCHASE, 5, NOW);

        List<InteractionPersistencePort.SimilarityRow> rows = adapter.computeItemSimilarity(
                NOW.minus(Duration.ofDays(180)), strongTypes(), 1);

        assertThat(rows.getFirst().coOccurrence()).isEqualTo(2);
    }

    @Test
    void onlyPurchasesCountAsOwnership() {
        persist("user-1", "p-bought", InteractionType.PURCHASE, 5, NOW);
        persist("user-1", "p-carted", InteractionType.CART_ADD, 4, NOW);
        persist("user-1", "p-viewed", InteractionType.VIEW, 1, NOW);

        assertThat(adapter.findPurchasedProductIds("user-1")).containsExactly("p-bought");
    }

    @Test
    void pruneDeletesInBoundedBatches() {
        for (int index = 0; index < 5; index++) {
            persist("user-1", "p-" + index, InteractionType.VIEW, 1, NOW.minus(Duration.ofDays(400)));
        }

        int firstBatch = adapter.deleteOlderThan(NOW.minus(Duration.ofDays(180)), 2);

        assertThat(firstBatch).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void erasingAUserRemovesEveryInteractionTheyProduced() {
        persist("user-1", "p-1", InteractionType.VIEW, 1, NOW);
        persist("user-1", "p-2", InteractionType.PURCHASE, 5, NOW);
        persist("user-2", "p-1", InteractionType.VIEW, 1, NOW);

        assertThat(adapter.deleteByUser("user-1")).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void usersAreListedOnceRegardlessOfActivityVolume() {
        persist("user-1", "p-1", InteractionType.VIEW, 1, NOW);
        persist("user-1", "p-2", InteractionType.VIEW, 1, NOW);
        persist("user-2", "p-1", InteractionType.VIEW, 1, NOW);
        persist("user-3", "p-1", InteractionType.VIEW, 1, NOW.minus(Duration.ofDays(10)));

        assertThat(adapter.findUserIdsWithInteractionsSince(NOW.minus(Duration.ofDays(1)), 100))
                .containsExactly("user-1", "user-2");
    }

    @Test
    void appendedInteractionsComeBackNewestFirst() {
        persist("user-1", "p-old", InteractionType.VIEW, 1, NOW.minus(Duration.ofDays(5)));
        persist("user-1", "p-new", InteractionType.VIEW, 1, NOW);

        assertThat(adapter.findByUser("user-1", NOW.minus(Duration.ofDays(30)), 10))
                .extracting(UserInteraction::getProductId)
                .containsExactly("p-new", "p-old");
    }

    private void persist(
            String userId, String productId, InteractionType type, double weight, Instant occurredAt) {
        repository.save(InteractionEntity.builder()
                .interactionId(IdGenerator.ulid())
                .userId(userId)
                .productId(productId)
                .interactionType(type.name())
                .weight(BigDecimal.valueOf(weight))
                .occurredAt(occurredAt)
                .createdAt(NOW)
                .build());
    }

    private static Map<InteractionType, Long> halfLives() {
        Map<InteractionType, Long> halfLives = new EnumMap<>(InteractionType.class);
        halfLives.put(InteractionType.VIEW, Duration.ofDays(14).getSeconds());
        halfLives.put(InteractionType.CART_ADD, Duration.ofDays(30).getSeconds());
        halfLives.put(InteractionType.PURCHASE, Duration.ofDays(180).getSeconds());
        return halfLives;
    }

    private static List<InteractionType> strongTypes() {
        return List.of(InteractionType.CART_ADD, InteractionType.PURCHASE);
    }
}
