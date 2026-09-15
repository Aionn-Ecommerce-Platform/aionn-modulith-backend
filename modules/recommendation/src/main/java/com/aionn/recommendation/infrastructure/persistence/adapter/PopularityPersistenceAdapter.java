package com.aionn.recommendation.infrastructure.persistence.adapter;

import com.aionn.recommendation.application.port.out.PopularityPersistencePort;
import com.aionn.recommendation.domain.model.ProductPopularity;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.persistence.mapper.RecommendationSignalDomainMapper;
import com.aionn.recommendation.infrastructure.persistence.repository.ProductPopularityRepository;
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
 * Writes decayed popularity in bounded batches, each committed on its own.
 *
 * <p>Same reasoning as the similarity adapter: one statement per row inside a single transaction on
 * the application-wide timeout meant the refresh started failing as soon as the catalogue was large
 * enough for the scores to matter, and failed silently apart from an error log.
 */
@Slf4j
@Component
public class PopularityPersistenceAdapter implements PopularityPersistencePort {

    private static final String UPSERT_SQL = """
            INSERT INTO recommendation_popularity
                (product_id, popularity_score, view_count, purchase_count, computed_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (product_id) DO UPDATE SET
                popularity_score = EXCLUDED.popularity_score,
                view_count       = EXCLUDED.view_count,
                purchase_count   = EXCLUDED.purchase_count,
                computed_at      = EXCLUDED.computed_at
            """;

    private static final String DELETE_STALE_SQL =
            "DELETE FROM recommendation_popularity WHERE computed_at < ?";

    private final ProductPopularityRepository jpa;
    private final RecommendationSignalDomainMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate writeBatch;
    private final int batchSize;

    public PopularityPersistenceAdapter(
            ProductPopularityRepository jpa,
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
    public List<ProductPopularity> findTop(int limit) {
        return jpa.findTop(Math.max(1, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ProductPopularity> findByProductIds(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return jpa.findByProductIdIn(productIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void upsertAll(List<ProductPopularity> popularities) {
        if (popularities == null || popularities.isEmpty()) {
            return;
        }
        int total = popularities.size();
        for (int from = 0; from < total; from += batchSize) {
            List<ProductPopularity> chunk = popularities.subList(from, Math.min(from + batchSize, total));
            writeBatch.executeWithoutResult(status -> writeChunk(chunk));
        }
        log.debug("Upserted {} popularity row(s) in batches of {}", total, batchSize);
    }

    private void writeChunk(List<ProductPopularity> chunk) {
        jdbcTemplate.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                ProductPopularity popularity = chunk.get(index);
                statement.setString(1, popularity.productId());
                BigDecimal score = popularity.score();
                if (score == null) {
                    statement.setNull(2, Types.NUMERIC);
                } else {
                    statement.setBigDecimal(2, score);
                }
                statement.setLong(3, popularity.viewCount());
                statement.setLong(4, popularity.purchaseCount());
                Instant computedAt = popularity.computedAt();
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
        return writeBatch.execute(status ->
                jdbcTemplate.update(DELETE_STALE_SQL, Timestamp.from(cutoff)));
    }
}
