package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.RecommendationCachePort;
import com.aionn.recommendation.application.port.out.UserAffinityProfilePersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionRetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    @Mock private InteractionPersistencePort interactionRepository;
    @Mock private UserAffinityProfilePersistencePort profileRepository;
    @Mock private RecommendationCachePort cache;

    private InteractionRetentionService service() {
        return new InteractionRetentionService(
                interactionRepository,
                profileRepository,
                cache,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void pruningIsMeasuredBackwardsFromNowRatherThanAStoredCutoff() {
        when(interactionRepository.deleteOlderThan(eq(NOW.minus(Duration.ofDays(180))), anyInt()))
                .thenReturn(7);

        assertThat(service().prune(Duration.ofDays(180), 500)).isEqualTo(7);
    }

    @Test
    void theBatchSizeReachesTheRepositorySoASweepNeverLocksTheWholeTable() {
        // The ingest path writes to the same table; an unbounded delete would block it for the length of
        // the sweep.
        service().prune(Duration.ofDays(180), 500);

        verify(interactionRepository).deleteOlderThan(NOW.minus(Duration.ofDays(180)), 500);
    }

    @Test
    void erasingAnAccountRemovesBothTheRawSignalsAndTheProfileDerivedFromThem() {
        // Deleting only the interactions would leave a profile that still describes the person.
        service().eraseUser("user-1");

        verify(interactionRepository).deleteByUser("user-1");
        verify(profileRepository).deleteByUserId("user-1");
    }

    @Test
    void theCachedFeedIsEvictedAfterTheDataBehindItIsGone() {
        // Evicting first would let an in-flight request repopulate the entry from rows still present.
        service().eraseUser("user-1");

        InOrder order = inOrder(interactionRepository, profileRepository, cache);
        order.verify(interactionRepository).deleteByUser("user-1");
        order.verify(profileRepository).deleteByUserId("user-1");
        order.verify(cache).evictUserFeed("user-1");
    }

    @Test
    void anAccountWithNothingStoredIsStillEvictedFromTheCache() {
        // A user who never generated a signal can still hold a cached trending-derived feed.
        when(interactionRepository.deleteByUser("user-1")).thenReturn(0);
        when(profileRepository.deleteByUserId("user-1")).thenReturn(0);

        service().eraseUser("user-1");

        verify(cache).evictUserFeed("user-1");
    }

    @Test
    void erasingLeavesAMarkSoTheDataCannotBeWrittenBack() {
        // Deleting the rows is not enough: afterwards nothing distinguishes an erased account from one
        // that never had any behavioural data, so a replayed event or a stale refresh sweep recreates
        // what was just erased.
        service().eraseUser("user-1");

        verify(interactionRepository).markUserErased("user-1", NOW);
    }

    @Test
    void theLockIsTakenBeforeAnythingIsReadOrWritten() {
        // A profile refresh part-way through reading this user's interactions would otherwise commit the
        // profile back after the delete, and an ingest that checked for the mark a moment earlier would
        // append a row the erasure has already finished removing.
        service().eraseUser("user-1");

        InOrder order = inOrder(interactionRepository, profileRepository);
        order.verify(interactionRepository).lockUser("user-1");
        order.verify(interactionRepository).markUserErased("user-1", NOW);
        order.verify(interactionRepository).deleteByUser("user-1");
        order.verify(profileRepository).deleteByUserId("user-1");
    }
}
