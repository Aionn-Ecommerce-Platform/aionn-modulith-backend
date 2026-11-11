package com.aionn.recommendation.infrastructure.persistence.adapter;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.domain.model.UserInteraction;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.persistence.mapper.InteractionDomainMapper;
import com.aionn.recommendation.infrastructure.persistence.repository.InteractionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InteractionPersistenceAdapter implements InteractionPersistencePort {

    /** Postgres caps bind parameters at 65535; this is well under it. */
    private static final int MAX_USER_INTERACTIONS = 10_000;

    private final InteractionRepository jpa;
    private final InteractionDomainMapper mapper;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    /**
     * Read-only template for the offline rebuilds, built here rather than as a bean so Boot still
     * supplies the unqualified {@code TransactionTemplate} other modules inject. The timeout is
     * longer than the request default because both rebuilds scan the interaction log.
     */
    private final TransactionTemplate computeReads;

    public InteractionPersistenceAdapter(
            InteractionRepository jpa,
            InteractionDomainMapper mapper,
            JdbcTemplate jdbc,
            Clock clock,
            PlatformTransactionManager transactionManager,
            RecommendationJobProperties jobProperties) {
        this.jpa = jpa;
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.clock = clock;
        this.computeReads = new TransactionTemplate(transactionManager);
        this.computeReads.setReadOnly(true);
        this.computeReads.setTimeout(jobProperties.execution().computeTimeoutSeconds());
    }

    @Override
    public void append(UserInteraction interaction) {
        if (interaction == null) {
            throw new IllegalArgumentException("interaction must not be null");
        }
        int written = jpa.appendIdempotent(mapper.toEntity(interaction, clock.instant()));
        if (written == 0) {
            // Replay of an already-ingested event; common under retry storms.
            log.debug("Ignored duplicate interaction from source event {}",
                    interaction.getSourceEventId());
        }
    }

    @Override
    public List<UserInteraction> findByUser(String userId, Instant since, int limit) {
        int safeLimit = limit <= 0 || limit > MAX_USER_INTERACTIONS ? MAX_USER_INTERACTIONS : limit;
        return jpa.findByUserSince(userId, since, safeLimit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<String> findPurchasedProductIds(String userId) {
        return jpa.findPurchasedProductIds(userId);
    }

    @Override
    public List<String> findUserIdsWithInteractionsSince(Instant since, int limit) {
        return jpa.findUserIdsWithInteractionsSince(since, Math.max(1, limit));
    }

    @Override
    public Map<String, PopularityAggregate> aggregatePopularity(
            Instant since, Instant now, Map<InteractionType, Long> halfLifeSeconds) {

        List<InteractionRepository.PopularityProjection> rows = computeReads.execute(status ->
                jpa.aggregatePopularity(
                        since,
                        now,
                        halfLifeSeconds.get(InteractionType.VIEW),
                        halfLifeSeconds.get(InteractionType.CART_ADD),
                        halfLifeSeconds.get(InteractionType.PURCHASE)));

        Map<String, PopularityAggregate> aggregates = LinkedHashMap.newLinkedHashMap(rows.size());
        for (InteractionRepository.PopularityProjection row : rows) {
            BigDecimal score = row.getDecayedScore() == null ? BigDecimal.ZERO : row.getDecayedScore();
            aggregates.put(row.getProductId(), new PopularityAggregate(
                    score, row.getViewCount(), row.getPurchaseCount()));
        }
        return aggregates;
    }

    @Override
    public List<SimilarityRow> computeItemSimilarity(
            Instant since,
            Collection<InteractionType> strongTypes,
            int minCoOccurrence,
            int maxNeighboursPerProduct) {
        if (strongTypes == null || strongTypes.isEmpty()) {
            return List.of();
        }
        List<String> typeNames = strongTypes.stream().map(InteractionType::name).toList();
        List<InteractionRepository.SimilarityProjection> rows = computeReads.execute(status ->
                jpa.computeItemSimilarity(
                        since, typeNames, Math.max(1, minCoOccurrence),
                        Math.max(1, maxNeighboursPerProduct)));

        List<SimilarityRow> similarities = new ArrayList<>(rows.size());
        for (InteractionRepository.SimilarityProjection row : rows) {
            similarities.add(new SimilarityRow(
                    row.getProductId(), row.getSimilarProductId(), row.getScore(), row.getCoOccurrence()));
        }
        return similarities;
    }

    @Override
    public int deleteOlderThan(Instant cutoff, int batchSize) {
        return jpa.deleteOlderThan(cutoff, Math.max(1, batchSize));
    }

    @Override
    public PrunedInteractions pruneOlderThan(Instant cutoff, int batchSize) {
        int safeBatch = Math.max(1, batchSize);
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT interaction_id, user_id, product_id
                  FROM recommendation_interactions
                 WHERE occurred_at < ?
                 ORDER BY occurred_at
                 LIMIT ?
                """, (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getString("interaction_id"));
                    row.put("user", rs.getString("user_id"));
                    row.put("product", rs.getString("product_id"));
                    return row;
                }, Timestamp.from(cutoff), safeBatch);
        if (rows.isEmpty()) {
            return new PrunedInteractions(0, List.of(), List.of());
        }

        List<String> ids = rows.stream().map(row -> (String) row.get("id")).toList();
        List<String> users = rows.stream().map(row -> (String) row.get("user"))
                .filter(v -> v != null).distinct().toList();
        List<String> products = rows.stream().map(row -> (String) row.get("product"))
                .filter(v -> v != null).distinct().toList();
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        int deleted = jdbc.update(
                "DELETE FROM recommendation_interactions WHERE interaction_id IN (" + placeholders + ")",
                ids.toArray());
        return new PrunedInteractions(deleted, users, products);
    }

    @Override
    public int deleteByUser(String userId) {
        return jpa.deleteByUserId(userId);
    }

    @Override
    public void lockUser(String userId) {
        jpa.lockUser(userId);
    }

    @Override
    public void markUserErased(String userId, Instant erasedAt) {
        jpa.markUserErased(userId, erasedAt);
    }

    @Override
    public boolean isUserErased(String userId) {
        return userId != null && jpa.isUserErased(userId);
    }
}
