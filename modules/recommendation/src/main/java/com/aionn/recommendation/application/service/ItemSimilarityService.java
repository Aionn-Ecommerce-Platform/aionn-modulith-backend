package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.ItemSimilarityPersistencePort;
import com.aionn.recommendation.domain.model.ItemSimilarity;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Rebuilds the item-to-item similarity matrix.
 *
 * <p>Item-based rather than user-based: the item matrix is smaller, changes more slowly, and can be
 * precomputed. A user-based matrix would need recomputing whenever anyone acts, and a new user would
 * have no neighbours at all.
 *
 * <p>Only strong signals count. Views are excluded because browsing is exploratory - two products
 * appearing in the same browsing session says far less than two products appearing in the same
 * basket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemSimilarityService {

    private final InteractionPersistencePort interactionRepository;
    private final ItemSimilarityPersistencePort similarityRepository;
    private final Clock clock;

    @Transactional
    public int refresh(Duration lookback, int minCoOccurrence) {
        Instant now = clock.instant();
        Instant since = now.minus(lookback);
        Collection<InteractionType> strongTypes = Arrays.stream(InteractionType.values())
                .filter(InteractionType::isStrongSignal)
                .toList();

        List<InteractionPersistencePort.SimilarityRow> rows =
                interactionRepository.computeItemSimilarity(since, strongTypes, minCoOccurrence);
        if (rows.isEmpty()) {
            log.debug("No product pairs met the co-occurrence threshold of {}", minCoOccurrence);
            return 0;
        }

        // Each unordered pair is stored in both directions so a neighbour lookup needs a single
        // indexed predicate instead of an OR across two columns.
        List<ItemSimilarity> similarities = new ArrayList<>(rows.size() * 2);
        for (InteractionPersistencePort.SimilarityRow row : rows) {
            AffinityScore score = AffinityScore.of(clampToUnit(row.score()));
            similarities.add(new ItemSimilarity(
                    row.productId(), row.similarProductId(), score, row.coOccurrence(), now));
            similarities.add(new ItemSimilarity(
                    row.similarProductId(), row.productId(), score, row.coOccurrence(), now));
        }

        similarityRepository.upsertAll(similarities);
        // Pairs that no longer co-occur within the window must disappear, otherwise a one-off
        // co-purchase from months ago would keep recommending forever.
        int removed = similarityRepository.deleteComputedBefore(now);
        log.info("Refreshed {} similarity pair(s); dropped {} stale pair(s)", rows.size(), removed);
        return rows.size();
    }

    /**
     * Floating-point cosine can land a hair outside [0,1]; clamping keeps the value object's
     * invariant intact without hiding a real computation error, since the excess is at rounding scale.
     */
    private static BigDecimal clampToUnit(double score) {
        return BigDecimal.valueOf(Math.clamp(score, 0.0d, 1.0d));
    }
}
