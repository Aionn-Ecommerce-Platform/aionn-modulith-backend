package com.aionn.recommendation.infrastructure.persistence.adapter;

import com.aionn.recommendation.application.port.out.ItemSimilarityPersistencePort;
import com.aionn.recommendation.domain.model.ItemSimilarity;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.persistence.mapper.RecommendationSignalDomainMapper;
import com.aionn.recommendation.infrastructure.persistence.repository.ItemSimilarityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Writes the similarity matrix in bounded batches, each committed on its own.
 *
 * <p>The previous shape - one {@code @Modifying} statement per row inside a single transaction - did
 * not survive contact with a real catalogue. Every pair is stored in both directions, so a rebuild of
 * P pairs issued 2P separate round trips, and the whole thing sat inside a transaction carrying the
 * application-wide {@code spring.transaction.default-timeout}. Past a few thousand pairs the timeout
 * fired, the transaction rolled back, and the scheduler logged one error line: the matrix stayed
 * empty forever while looking like a job that had merely not run yet.
 *
 * <p>Committing per batch trades all-or-nothing for progressive application. That is the right trade
 * here: a partially refreshed matrix recommends slightly stale neighbours until the next run, whereas
 * a transaction that always times out recommends nothing at all. Each pair upserts on its natural key,
 * so a partial run is also safe to re-run.
 */
@Slf4j
@Component
public class ItemSimilarityPersistenceAdapter implements ItemSimilarityPersistencePort {

    private static final String UPSERT_SQL = """
            INSERT INTO recommendation_item_similarity
                (product_id, similar_product_id, score, co_occurrence, computed_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (product_id, similar_product_id) DO UPDATE SET
                score         = EXCLUDED.score,
                co_occurrence = EXCLUDED.co_occurrence,
                computed_at   = EXCLUDED.computed_at
            """;

    private static final String DELETE_STALE_SQL =
            "DELETE FROM recommendation_item_similarity WHERE computed_at < ?";

    private final ItemSimilarityRepository jpa;
    private final RecommendationSignalDomainMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate writeBatch;
    private final int batchSize;

    public ItemSimilarityPersistenceAdapter(
            ItemSimilarityRepository jpa,
            RecommendationSignalDomainMapper mapper,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            RecommendationJobProperties jobProperties) {
        this.jpa = jpa;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
        this.batchSize = jobProperties.execution().upsertBatchSize();
        this.writeBatch = new TransactionTemplate(transactionManager);
        this.writeBatch.setTimeout(jobProperties.execution().computeTimeoutSeconds());
    }

    @Override
    public List<ItemSimilarity> findNeighbours(String productId, int limit) {
        return jpa.findNeighbours(productId, Math.max(1, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ItemSimilarity> findNeighbours(Collection<String> productIds, int limitPerProduct) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return jpa.findNeighboursForAll(
                        productIds.toArray(String[]::new), Math.max(1, limitPerProduct))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void upsertAll(List<ItemSimilarity> similarities) {
        if (similarities == null || similarities.isEmpty()) {
            return;
        }
        int total = similarities.size();
        for (int from = 0; from < total; from += batchSize) {
            List<ItemSimilarity> chunk = similarities.subList(from, Math.min(from + batchSize, total));
            writeBatch.executeWithoutResult(status -> writeChunk(chunk));
        }
        log.debug("Upserted {} similarity row(s) in batches of {}", total, batchSize);
    }

    private void writeChunk(List<ItemSimilarity> chunk) {
        jdbcTemplate.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                ItemSimilarity similarity = chunk.get(index);
                statement.setString(1, similarity.productId());
                statement.setString(2, similarity.similarProductId());
                BigDecimal score = similarity.score().value();
                if (score == null) {
                    statement.setNull(3, Types.NUMERIC);
                } else {
                    statement.setBigDecimal(3, score);
                }
                statement.setInt(4, similarity.coOccurrence());
                Instant computedAt = similarity.computedAt();
                if (computedAt == null) {
                    statement.setNull(5, Types.TIMESTAMP);
                } else {
                    statement.setTimestamp(5, Timestamp.from(computedAt));
                }
            }

            @Override
            public int getBatchSize() {
                return chunk.size();
            }
        });
    }

    @Override
    public int deleteComputedBefore(Instant cutoff) {
        Integer deleted = writeBatch.execute(status ->
                jdbcTemplate.update(DELETE_STALE_SQL, Timestamp.from(cutoff)));
        return deleted == null ? 0 : deleted;
    }
}
