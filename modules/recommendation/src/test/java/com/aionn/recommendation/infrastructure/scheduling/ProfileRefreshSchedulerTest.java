package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationSchedulingProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.aionn.recommendation.infrastructure.scheduling.ProfileRefreshScheduler.JOB_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileRefreshSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
    private static final long DELAY_MS = Duration.ofMinutes(15).toMillis();
    private static final int BATCH_SIZE = 500;

    @Mock private InteractionPersistencePort interactionRepository;
    @Mock private ProfileRefreshWorker worker;
    @Mock private RecommendationMetricsPort metrics;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private ProfileRefreshScheduler scheduler(long lookbackDays) {
        return new ProfileRefreshScheduler(
                interactionRepository,
                worker,
                scheduling(),
                new RecommendationJobProperties(
                        new RecommendationJobProperties.Profile(10, lookbackDays),
                        new RecommendationJobProperties.Similarity(180, 2, 50),
                        new RecommendationJobProperties.Popularity(30),
                        new RecommendationJobProperties.Retention(180),
                        new RecommendationJobProperties.Execution(60, 500)),
                metrics,
                clock);
    }

    private static RecommendationSchedulingProperties scheduling() {
        RecommendationSchedulingProperties.Job job =
                new RecommendationSchedulingProperties.Job(true, DELAY_MS, BATCH_SIZE);
        return new RecommendationSchedulingProperties(job, job, job, job);
    }

    @Test
    void theSweepCoversAGapWiderThanOneInterval() {
        // A run that was delayed or skipped still has to account for the activity it missed, so the window
        // is several intervals rather than exactly one.
        when(interactionRepository.findUserIdsWithInteractionsSince(any(), anyInt()))
                .thenReturn(List.of("user-1"));
        when(worker.refreshOne(any())).thenReturn(true);

        scheduler(180).run();

        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(interactionRepository).findUserIdsWithInteractionsSince(since.capture(), eq(BATCH_SIZE));
        assertThat(since.getValue()).isEqualTo(NOW.minus(Duration.ofMillis(DELAY_MS * 3)));
    }

    @Test
    void anEmptySweepWidensTheWindowUntilItFindsSomebody() {
        // A quiet period, a restart after downtime, or activity that predates the module would otherwise
        // leave those users on cold-start recommendations until they happen to interact again - which for
        // a seeded catalogue means never.
        when(interactionRepository.findUserIdsWithInteractionsSince(any(), anyInt()))
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of("user-old"));
        when(worker.refreshOne("user-old")).thenReturn(true);

        scheduler(180).run();

        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(interactionRepository, times(3))
                .findUserIdsWithInteractionsSince(since.capture(), eq(BATCH_SIZE));
        List<Instant> windows = since.getAllValues();
        assertThat(windows.get(0)).isEqualTo(NOW.minus(Duration.ofMinutes(45)));
        assertThat(windows.get(1)).isEqualTo(NOW.minus(Duration.ofHours(6)));
        assertThat(windows.get(2)).isEqualTo(NOW.minus(Duration.ofDays(2)));
        verify(worker).refreshOne("user-old");
    }

    @Test
    void wideningStopsAtTheProfileLookback() {
        // Interactions older than the lookback are pruned and could not change the recomputed profile, so
        // scanning further back would cost a wider aggregate to find the same answer.
        when(interactionRepository.findUserIdsWithInteractionsSince(any(), anyInt()))
                .thenReturn(List.of());

        scheduler(180).run();

        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(interactionRepository, org.mockito.Mockito.atLeastOnce())
                .findUserIdsWithInteractionsSince(since.capture(), eq(BATCH_SIZE));
        assertThat(since.getAllValues()).allSatisfy(window ->
                assertThat(window).isAfterOrEqualTo(NOW.minus(Duration.ofDays(180))));
        assertThat(since.getAllValues().getLast()).isEqualTo(NOW.minus(Duration.ofDays(180)));
        verify(worker, never()).refreshOne(any());
        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(true));
    }

    @Test
    void aConfiguredDelayLongerThanTheLookbackDoesNotScanPastIt() {
        lenient().when(interactionRepository.findUserIdsWithInteractionsSince(any(), anyInt()))
                .thenReturn(List.of());
        RecommendationSchedulingProperties.Job job = new RecommendationSchedulingProperties.Job(
                true, Duration.ofDays(400).toMillis(), BATCH_SIZE);

        new ProfileRefreshScheduler(
                interactionRepository,
                worker,
                new RecommendationSchedulingProperties(job, job, job, job),
                new RecommendationJobProperties(
                        new RecommendationJobProperties.Profile(10, 180),
                        new RecommendationJobProperties.Similarity(180, 2, 50),
                        new RecommendationJobProperties.Popularity(30),
                        new RecommendationJobProperties.Retention(180),
                        new RecommendationJobProperties.Execution(60, 500)),
                metrics,
                clock).run();

        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(interactionRepository).findUserIdsWithInteractionsSince(since.capture(), eq(BATCH_SIZE));
        assertThat(since.getValue()).isEqualTo(NOW.minus(Duration.ofDays(180)));
    }

    @Test
    void oneFailingUserMarksTheWholeRunFailed() {
        // Otherwise a user whose refresh throws on every sweep is invisible: the sweep returns normally
        // and the worker only logs a warning, so the profile silently stops tracking that person.
        when(interactionRepository.findUserIdsWithInteractionsSince(any(), anyInt()))
                .thenReturn(List.of("user-1", "user-2"));
        when(worker.refreshOne("user-1")).thenReturn(true);
        when(worker.refreshOne("user-2")).thenReturn(false);

        scheduler(180).run();

        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(false));
    }

    @Test
    void everyUserIsRefreshedEvenWhenAnEarlierOneFails() {
        when(interactionRepository.findUserIdsWithInteractionsSince(any(), anyInt()))
                .thenReturn(List.of("user-1", "user-2"));
        when(worker.refreshOne("user-1")).thenReturn(false);
        when(worker.refreshOne("user-2")).thenReturn(true);

        scheduler(180).run();

        verify(worker).refreshOne("user-1");
        verify(worker).refreshOne("user-2");
    }

    @Test
    void aSweepThatThrowsIsRecordedAsFailed() {
        when(interactionRepository.findUserIdsWithInteractionsSince(any(), anyInt()))
                .thenThrow(new IllegalStateException("boom"));

        scheduler(180).run();

        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(false));
        verify(worker, never()).refreshOne(any());
    }
}
