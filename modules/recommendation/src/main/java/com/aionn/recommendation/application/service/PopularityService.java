package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.policy.InteractionWeightPolicy;
import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.PopularityPersistencePort;
import com.aionn.recommendation.domain.model.ProductPopularity;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Recomputes time-decayed popularity.
 *
 * <p>Answers "what has momentum right now", which is a different question from catalog's lifetime
 * {@code product_sold_counters}. A product can be a legitimate all-time best-seller and still not be
 * trending.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularityService {

    private final InteractionPersistencePort interactionRepository;
    private final PopularityPersistencePort popularityRepository;
    private final InteractionWeightPolicy weightPolicy;
    private final Clock clock;

    /**
     * Recomputes the table.
     *
     * <p>Not {@code @Transactional}: the aggregate read and each write batch open their own short
     * transaction in the persistence adapter. Wrapping the whole sweep in one transaction put it on the
     * application-wide request-sized timeout, which the aggregate plus a per-product upsert exceeds
     * once the interaction log is worth aggregating.
     */
    public int refresh(Duration lookback) {
        Instant now = clock.instant();
        Instant since = now.minus(lookback);

        Map<InteractionType, Long> halfLives = new EnumMap<>(InteractionType.class);
        for (InteractionType type : InteractionType.values()) {
            halfLives.put(type, weightPolicy.weightFor(type).halfLife().getSeconds());
        }

        Map<String, InteractionPersistencePort.PopularityAggregate> aggregates =
                interactionRepository.aggregatePopularity(since, now, halfLives);
        if (aggregates.isEmpty()) {
            log.debug("No interactions in the popularity window");
            // Still sweep. An empty window is a statement that nothing has momentum, and returning early
            // would leave the previous scores ranking products that stopped selling, indefinitely, for
            // want of a single new interaction anywhere in the catalogue.
            int stale = popularityRepository.deleteComputedBefore(now);
            if (stale > 0) {
                log.info("Dropped {} stale popularity row(s)", stale);
            }
            return 0;
        }

        List<ProductPopularity> popularities = new ArrayList<>(aggregates.size());
        aggregates.forEach((productId, aggregate) -> popularities.add(new ProductPopularity(
                productId,
                aggregate.decayedScore(),
                aggregate.viewCount(),
                aggregate.purchaseCount(),
                now)));

        popularityRepository.upsertAll(popularities);
        // Products with no interaction in the window keep no row: their momentum is genuinely zero,
        // and leaving stale rows behind would let them outrank newly active products.
        int removed = popularityRepository.deleteComputedBefore(now);
        log.info("Refreshed popularity for {} product(s); dropped {} stale row(s)",
                popularities.size(), removed);
        return popularities.size();
    }

    @Transactional(readOnly = true)
    public List<ProductPopularity> top(int limit) {
        return popularityRepository.findTop(limit);
    }
}
