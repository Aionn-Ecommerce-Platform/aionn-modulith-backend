package com.aionn.recommendation.infrastructure.persistence;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.domain.model.UserInteraction;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.persistence.adapter.InteractionPersistenceAdapter;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the SQL that carries real semantics: exponential decay, the item-item cosine, the ingest
 * conflict guard and the profile-refresh sweep. All of them live in native queries, so unit tests over
 * a mocked repository would prove nothing about them.
 *
 * <p>Runs against the Flyway schema rather than {@code ddl-auto: create-drop}. Hibernate generates DDL
 * from the entity annotations, which carry no CHECK constraints and cannot express a partial unique
 * index at all - so a create-drop schema would silently pass an ingest that the production schema
 * rejects, and reject nothing that it should.
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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db");
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        /**
         * The adapter builds its own read-only {@code TransactionTemplate} from these bounds, so the
         * slice needs the record even though nothing here exercises the values.
         */
        @Bean
        RecommendationJobProperties jobProperties() {
            return new RecommendationJobProperties(
                    new RecommendationJobProperties.Profile(10, 180),
                    new RecommendationJobProperties.Similarity(180, 2, 50),
                    new RecommendationJobProperties.Popularity(30),
                    new RecommendationJobProperties.Retention(180),
                    new RecommendationJobProperties.Execution(60, 500));
        }
    }

    @Autowired
    private InteractionRepository repository;
    @Autowired
    private InteractionPersistenceAdapter adapter;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM recommendation_interactions");
        jdbcTemplate.update("DELETE FROM recommendation_user_profiles");
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
                NOW.minus(Duration.ofDays(180)), strongTypes(), 1, 50);

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
                NOW.minus(Duration.ofDays(180)), strongTypes(), 1, 50)).hasSize(1);
    }

    @Test
    void pairsBelowTheCoOccurrenceFloorAreDiscarded() {
        // One shared buyer yields a high cosine that means nothing; the floor keeps it out.
        persist("user-1", "p-1", InteractionType.PURCHASE, 5, NOW);
        persist("user-1", "p-2", InteractionType.PURCHASE, 5, NOW);

        assertThat(adapter.computeItemSimilarity(
                NOW.minus(Duration.ofDays(180)), strongTypes(), 2, 50)).isEmpty();
    }

    @Test
    void viewsDoNotContributeToSimilarity() {
        persist("user-1", "p-1", InteractionType.VIEW, 1, NOW);
        persist("user-1", "p-2", InteractionType.VIEW, 1, NOW);
        persist("user-2", "p-1", InteractionType.VIEW, 1, NOW);
        persist("user-2", "p-2", InteractionType.VIEW, 1, NOW);

        assertThat(adapter.computeItemSimilarity(
                NOW.minus(Duration.ofDays(180)), strongTypes(), 1, 50)).isEmpty();
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
                NOW.minus(Duration.ofDays(180)), strongTypes(), 1, 50);

        assertThat(rows.getFirst().coOccurrence()).isEqualTo(2);
    }

    @Test
    void theNeighbourCapBoundsEveryProductNotJustOneDirection() {
        // The caller writes each returned pair both ways, so capping one direction alone would not bound
        // the stored table. Four users buy a hub plus a narrowing tail of products; capping at two keeps
        // the hub's strongest pairs and drops the rest, including pairs between tail products that the
        // hub no longer points at.
        persist("user-1", "p-hub", InteractionType.PURCHASE, 5, NOW);
        persist("user-2", "p-hub", InteractionType.PURCHASE, 5, NOW);
        persist("user-3", "p-hub", InteractionType.PURCHASE, 5, NOW);
        persist("user-4", "p-hub", InteractionType.PURCHASE, 5, NOW);
        for (String user : List.of("user-1", "user-2", "user-3", "user-4")) {
            persist(user, "p-1", InteractionType.PURCHASE, 5, NOW);
        }
        for (String user : List.of("user-1", "user-2", "user-3")) {
            persist(user, "p-2", InteractionType.PURCHASE, 5, NOW);
        }
        for (String user : List.of("user-1", "user-2")) {
            persist(user, "p-3", InteractionType.PURCHASE, 5, NOW);
        }
        persist("user-1", "p-4", InteractionType.PURCHASE, 5, NOW);

        List<InteractionPersistencePort.SimilarityRow> rows = adapter.computeItemSimilarity(
                NOW.minus(Duration.ofDays(180)), strongTypes(), 1, 2);

        // p-1 <-> p-2 <-> p-hub and p-1 <-> p-hub; the two weakest products lose every neighbour.
        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(InteractionPersistencePort.SimilarityRow::productId)
                .containsOnly("p-1", "p-2");
        assertThat(rows).extracting(InteractionPersistencePort.SimilarityRow::similarProductId)
                .doesNotContain("p-3", "p-4");
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
    void delayedIngestionStillRefreshesWhenBusinessTimePredatesTheProfile() {
        // A delayed outbox delivery can carry an old business timestamp but is new behavioural data.
        // Staleness must therefore use ingestion time while the lookback still uses occurred_at.
        saveProfile("user-delayed", NOW.minus(Duration.ofHours(1)));
        adapter.append(UserInteraction.create(
                IdGenerator.ulid(), "user-delayed", "p-1", InteractionType.VIEW, BigDecimal.ONE,
                NOW.minus(Duration.ofHours(2)), "evt-delayed"));

        assertThat(adapter.findUserIdsWithInteractionsSince(NOW.minus(Duration.ofDays(1)), 100))
                .containsExactly("user-delayed");

        jdbcTemplate.update("UPDATE recommendation_user_profiles SET refreshed_at = ? WHERE user_id = ?",
                Timestamp.from(NOW), "user-delayed");
        assertThat(adapter.findUserIdsWithInteractionsSince(NOW.minus(Duration.ofDays(1)), 100))
                .isEmpty();
    }

    @Test
    void delayedIngestionOutsideTheBusinessTimeLookbackDoesNotRefreshTheProfile() {
        saveProfile("user-delayed", NOW.minus(Duration.ofHours(1)));
        adapter.append(UserInteraction.create(
                IdGenerator.ulid(), "user-delayed", "p-1", InteractionType.VIEW, BigDecimal.ONE,
                NOW.minus(Duration.ofDays(2)), "evt-too-old"));

        assertThat(adapter.findUserIdsWithInteractionsSince(NOW.minus(Duration.ofDays(1)), 100))
                .isEmpty();
    }

    @Test
    void aProfileNewerThanTheUsersLatestIngestionDropsOutOfTheSweep() {
        // This is what makes the batch rotate. A plain ORDER BY user_id LIMIT returns the same prefix
        // every run, so once active users exceed the batch size everyone past it is never refreshed.
        persist("user-current", "p-1", InteractionType.VIEW, 1,
                NOW.minus(Duration.ofHours(2)), NOW.minus(Duration.ofHours(2)));
        persist("user-stale", "p-1", InteractionType.VIEW, 1,
                NOW.minus(Duration.ofHours(2)), NOW.minus(Duration.ofHours(2)));
        saveProfile("user-current", NOW.minus(Duration.ofHours(1)));
        saveProfile("user-stale", NOW.minus(Duration.ofHours(3)));

        assertThat(adapter.findUserIdsWithInteractionsSince(NOW.minus(Duration.ofDays(1)), 100))
                .containsExactly("user-stale");
    }

    @Test
    void aUserWithNoProfileRowAlwaysQualifies() {
        // The LEFT JOIN has to keep users the profile table has never heard of, which is every user on
        // the first sweep after the module is enabled.
        persist("user-new", "p-1", InteractionType.VIEW, 1, NOW.minus(Duration.ofHours(1)));
        saveProfile("user-refreshed", NOW);
        persist("user-refreshed", "p-1", InteractionType.VIEW, 1, NOW.minus(Duration.ofHours(1)));

        assertThat(adapter.findUserIdsWithInteractionsSince(NOW.minus(Duration.ofDays(1)), 100))
                .containsExactly("user-new");
    }

    @Test
    void theBatchLimitIsAppliedAfterStaleUsersAreSelected() {
        for (int index = 0; index < 5; index++) {
            persist("user-" + index, "p-1", InteractionType.VIEW, 1,
                    NOW.minus(Duration.ofHours(1)), NOW.minus(Duration.ofMinutes(4 - index)));
        }

        // Order by ingestion time rather than business time or the user ID. Most recent ingestion
        // wins even when all events occurred together.
        assertThat(adapter.findUserIdsWithInteractionsSince(NOW.minus(Duration.ofDays(1)), 2))
                .containsExactly("user-4", "user-3");
    }

    @Test
    void appendedInteractionsComeBackNewestFirst() {
        persist("user-1", "p-old", InteractionType.VIEW, 1, NOW.minus(Duration.ofDays(5)));
        persist("user-1", "p-new", InteractionType.VIEW, 1, NOW);

        assertThat(adapter.findByUser("user-1", NOW.minus(Duration.ofDays(30)), 10))
                .extracting(UserInteraction::getProductId)
                .containsExactly("p-new", "p-old");
    }

    @Test
    void aRedeliveredEventWritesOneSignalNotTwo() {
        // Outbox delivery is at-least-once and the ingest listener commits in its own transaction, so a
        // lost inbox receipt replays the event. Each replay generates a fresh interaction ID, so only the
        // source-event index can stop the duplicate - and because popularity and affinity both sum over
        // this log, one duplicate inflates every score it touches permanently.
        adapter.append(interaction("user-1", "p-1", InteractionType.PURCHASE, "evt-1"));
        adapter.append(interaction("user-1", "p-1", InteractionType.PURCHASE, "evt-1"));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void oneEventCanStillTouchSeveralProducts() {
        // An order with three lines arrives as a single event. Deduplicating on the event alone would
        // drop two of the three purchases; the key includes the product for exactly this reason.
        adapter.append(interaction("user-1", "p-1", InteractionType.PURCHASE, "evt-order"));
        adapter.append(interaction("user-1", "p-2", InteractionType.PURCHASE, "evt-order"));
        adapter.append(interaction("user-1", "p-3", InteractionType.PURCHASE, "evt-order"));

        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void twoLinesForTheSameProductCollapseIntoOneSignal() {
        // Buying five of something is not liking it five times more, which is the rule already applied to
        // quantity within a line.
        adapter.append(interaction("user-1", "p-1", InteractionType.PURCHASE, "evt-order"));
        adapter.append(interaction("user-1", "p-1", InteractionType.PURCHASE, "evt-order"));

        List<UserInteraction> stored =
                adapter.findByUser("user-1", NOW.minus(Duration.ofDays(1)), 10);
        assertThat(stored).hasSize(1);
    }

    @Test
    void differentEventsForTheSameActionAreBothGenuineRepeatBehaviour() {
        adapter.append(interaction("user-1", "p-1", InteractionType.PURCHASE, "evt-1"));
        adapter.append(interaction("user-1", "p-1", InteractionType.PURCHASE, "evt-2"));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void interactionsWithNoSourceEventAreNeverDeduplicated() {
        // Seeded fixtures and manual backfills have no originating event, and the unique index is partial
        // so it leaves them unconstrained rather than collapsing unrelated rows onto a shared NULL.
        adapter.append(interaction("user-1", "p-1", InteractionType.VIEW, null));
        adapter.append(interaction("user-1", "p-1", InteractionType.VIEW, null));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void theSourceEventIdSurvivesTheRoundTrip() {
        adapter.append(interaction("user-1", "p-1", InteractionType.CART_ADD, "evt-cart"));

        assertThat(adapter.findByUser("user-1", NOW.minus(Duration.ofDays(1)), 10).getFirst()
                .getSourceEventId()).isEqualTo("evt-cart");
    }

    private void persist(
            String userId, String productId, InteractionType type, double weight, Instant occurredAt) {
        persist(userId, productId, type, weight, occurredAt, NOW);
    }

    private void persist(
            String userId, String productId, InteractionType type, double weight,
            Instant occurredAt, Instant createdAt) {
        repository.save(com.aionn.recommendation.infrastructure.persistence.entity.InteractionEntity
                .builder()
                .interactionId(IdGenerator.ulid())
                .userId(userId)
                .productId(productId)
                .interactionType(type.name())
                .weight(BigDecimal.valueOf(weight))
                .occurredAt(occurredAt)
                .createdAt(createdAt)
                .build());
    }

    /**
     * Appends through the adapter, so the row goes in by the same path production ingest uses. The
     * interaction ID is fresh on every call, which is what a replay does: only the source event repeats.
     */
    private UserInteraction interaction(
            String userId, String productId, InteractionType type, String sourceEventId) {
        return UserInteraction.create(
                IdGenerator.ulid(), userId, productId, type, BigDecimal.ONE, NOW, sourceEventId);
    }

    private void saveProfile(String userId, Instant refreshedAt) {
        jdbcTemplate.update("""
                INSERT INTO recommendation_user_profiles (user_id, refreshed_at)
                VALUES (?, ?)
                """, userId, Timestamp.from(refreshedAt));
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
