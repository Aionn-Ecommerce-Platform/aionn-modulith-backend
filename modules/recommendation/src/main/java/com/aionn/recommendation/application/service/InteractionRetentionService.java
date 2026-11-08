package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.RecommendationCachePort;
import com.aionn.recommendation.application.port.out.UserAffinityProfilePersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final InteractionPersistencePort interactionRepository;
    private final UserAffinityProfilePersistencePort profileRepository;
    private final RecommendationCachePort cache;
    private final Clock clock;

    /** Deletes in batches so the sweep never holds a long lock on a table the ingest path writes to. */
    @Transactional
    public int prune(Duration maxAge, int batchSize) {
        Instant cutoff = clock.instant().minus(maxAge);
        int deleted = interactionRepository.deleteOlderThan(cutoff, batchSize);
        if (deleted > 0) {
            log.info("Pruned {} interaction(s) older than {}", deleted, cutoff);
        }
        return deleted;
    }

    /** Erases everything held for one account, leaving a tombstone so replays and stale refreshes can't undo it. */
    @Transactional
    public void eraseUser(String userId) {
        interactionRepository.lockUser(userId);
        interactionRepository.markUserErased(userId, clock.instant());
        int interactions = interactionRepository.deleteByUser(userId);
        int profiles = profileRepository.deleteByUserId(userId);
        cache.evictUserFeed(userId);
        // Counts only: logging product IDs would put the erased history back into the log.
        log.info("Erased recommendation data for a deleted account: {} interaction(s), {} profile(s)",
                interactions, profiles);
    }
}
