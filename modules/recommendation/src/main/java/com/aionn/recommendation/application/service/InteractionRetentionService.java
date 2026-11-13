package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.ItemSimilarityPersistencePort;
import com.aionn.recommendation.application.port.out.PopularityPersistencePort;
import com.aionn.recommendation.application.port.out.RecommendationCachePort;
import com.aionn.recommendation.application.port.out.UserAffinityProfilePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Data lifecycle over the interaction log. Behavioural history is personal data with no accounting
 * value, so it is erased outright with the account; one tombstone row per erased account keeps late
 * events and stale refreshes from writing it back. Aggregate tables are not personally identifying.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionRetentionService {

    private final InteractionPersistencePort interactionPersistencePort;
    private final UserAffinityProfilePersistencePort userAffinityProfilePersistencePort;
    private final ItemSimilarityPersistencePort itemSimilarityPersistencePort;
    private final PopularityPersistencePort popularityPersistencePort;
    private final RecommendationCachePort cache;
    private final Clock clock;

    /** Deletes in batches so the sweep never holds a long lock on a table the ingest path writes to. */
    @Transactional
    public int prune(Duration maxAge, int batchSize) {
        Instant cutoff = clock.instant().minus(maxAge);
        InteractionPersistencePort.PrunedInteractions pruned =
                interactionPersistencePort.pruneOlderThan(cutoff, batchSize);
        if (pruned.deleted() == 0) {
            return 0;
        }
        userAffinityProfilePersistencePort.deleteByUserIds(pruned.userIds());
        itemSimilarityPersistencePort.deleteByProductIds(pruned.productIds());
        popularityPersistencePort.deleteByProductIds(pruned.productIds());
        afterCommit(() -> invalidateCache(pruned));
        log.info("Pruned {} interaction(s) older than {} and invalidated derived signals for {} user(s), {} product(s)",
                pruned.deleted(), cutoff, pruned.userIds().size(), pruned.productIds().size());
        return pruned.deleted();
    }

    /** Derived rows are rebuilt from the surviving window; stale cached slates must go with them. */
    private void invalidateCache(InteractionPersistencePort.PrunedInteractions pruned) {
        try {
            pruned.userIds().forEach(cache::evictUserFeed);
            pruned.productIds().forEach(cache::evictProduct);
            if (!pruned.productIds().isEmpty()) {
                cache.evictTrending();
            }
        } catch (RuntimeException ex) {
            log.warn("Could not invalidate recommendation cache after pruning: {}", ex.getMessage());
        }
    }

    /** Erases everything held for one account, leaving a tombstone so replays and stale refreshes can't undo it. */
    @Transactional
    public void eraseUser(String userId) {
        interactionPersistencePort.lockUser(userId);
        interactionPersistencePort.markUserErased(userId, clock.instant());
        int interactions = interactionPersistencePort.deleteByUser(userId);
        int profiles = userAffinityProfilePersistencePort.deleteByUserId(userId);
        afterCommit(() -> cache.evictUserFeed(userId));
        // Counts only: logging product IDs would put the erased history back into the log.
        log.info("Erased recommendation data for a deleted account: {} interaction(s), {} profile(s)",
                interactions, profiles);
    }

    /** Cache entries must not be evicted before the deletes commit, or an in-flight read can repopulate them. */
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
