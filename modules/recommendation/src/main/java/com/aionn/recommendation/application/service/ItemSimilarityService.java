package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.ItemSimilarityPersistencePort;
import com.aionn.recommendation.domain.model.ItemSimilarity;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Rebuilds the item-to-item similarity matrix. Item-based so it is smaller, slower-changing and
 * precomputable than a user-based matrix. Only strong signals count; views are too exploratory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemSimilarityService {

    private final InteractionPersistencePort interactionRepository;
    private final ItemSimilarityPersistencePort similarityRepository;
    private final Clock clock;

    /**
     * Deliberately not transactional: compute and each write batch use their own short transaction,
     * otherwise one long transaction exceeds the default timeout. A partial matrix is recovered by
     * the next run because every pair upserts on its natural key.
     */
    public int refresh(Duration lookback, int minCoOccurrence, int maxNeighboursPerProduct) {
        Instant now = clock.instant();
        Instant since = now.minus(lookback);
        Collection<InteractionType> strongTypes = Arrays.stream(InteractionType.values())
                .filter(InteractionType::isStrongSignal)
                .toList();

        List<InteractionPersistencePort.SimilarityRow> rows = interactionRepository
                .computeItemSimilarity(since, strongTypes, minCoOccurrence, maxNeighboursPerProduct);
        if (rows.isEmpty()) {
            log.debug("No product pairs met the co-occurrence threshold of {}", minCoOccurrence);
            // Still sweep: pairs that fell below the threshold must disappear.
            int stale = similarityRepository.deleteComputedBefore(now);
            if (stale > 0) {
                log.info("Dropped {} stale similarity pair(s)", stale);
            }
            return 0;
        }

        // Store both directions so lookups need one indexed predicate instead of an OR.
        List<ItemSimilarity> similarities = new ArrayList<>(rows.size() * 2);
        for (InteractionPersistencePort.SimilarityRow row : rows) {
            AffinityScore score = AffinityScore.of(clampToUnit(row.score()));
            similarities.add(new ItemSimilarity(
                    row.productId(), row.similarProductId(), score, row.coOccurrence(), now));
            similarities.add(new ItemSimilarity(
                    row.similarProductId(), row.productId(), score, row.coOccurrence(), now));
        }

        similarityRepository.upsertAll(similarities);
        // Drop pairs that no longer co-occur within the window.
        int removed = similarityRepository.deleteComputedBefore(now);
        log.info("Refreshed {} similarity pair(s); dropped {} stale pair(s)", rows.size(), removed);
        return rows.size();
    }

    /** Clamp rounding-scale float drift so the value object's [0,1] invariant holds. */
    private static BigDecimal clampToUnit(double score) {
        return BigDecimal.valueOf(Math.clamp(score, 0.0d, 1.0d));
    }
}
