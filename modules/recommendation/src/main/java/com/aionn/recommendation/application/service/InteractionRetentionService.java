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
 * Data-lifecycle operations over the interaction log.
 *
 * <p>Behavioural history is personal data with no accounting value, so unlike historical business
 * records it is deleted outright when an account is deleted rather than retained against a tombstoned
 * user ID. The aggregate tables - similarity and popularity - are not personally identifying and are
 * left alone.
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

    @Transactional
    public void eraseUser(String userId) {
        int interactions = interactionRepository.deleteByUser(userId);
        int profiles = profileRepository.deleteByUserId(userId);
        cache.evictUserFeed(userId);
        // Counts only - never the products involved, which would put the very history being erased into
        // the log.
        log.info("Erased recommendation data for a deleted account: {} interaction(s), {} profile(s)",
                interactions, profiles);
    }
}
