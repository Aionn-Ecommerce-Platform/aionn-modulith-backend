package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.application.service.InteractionRetentionService;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationSchedulingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static com.aionn.recommendation.infrastructure.scheduling.InteractionPruneScheduler.JOB_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class InteractionPruneSchedulerTest {

    @Mock
    private InteractionRetentionService retentionService;
    @Mock
    private RecommendationJobProperties jobProperties;
    @Mock
    private RecommendationSchedulingProperties schedulingProperties;
    @Mock
    private RecommendationJobProperties.Retention retention;
    @Mock
    private RecommendationSchedulingProperties.Job pruneJob;
    @Mock
    private RecommendationMetricsPort metrics;

    private InteractionPruneScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new InteractionPruneScheduler(
                retentionService, jobProperties, schedulingProperties, metrics);
    }

    @Test
    void runsSingleBatchWhenDeletedIsLessThanBatchSize() {
        when(schedulingProperties.prune()).thenReturn(pruneJob);
        when(pruneJob.batchSize()).thenReturn(100);
        when(jobProperties.retention()).thenReturn(retention);
        when(retention.interactionMaxAgeDays()).thenReturn(180L);
        when(retentionService.prune(Duration.ofDays(180), 100)).thenReturn(42);

        scheduler.run();

        verify(retentionService, times(1)).prune(Duration.ofDays(180), 100);
        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(true));
    }

    @Test
    void loopsUntilExpiredRowsAreDrained() {
        when(schedulingProperties.prune()).thenReturn(pruneJob);
        when(pruneJob.batchSize()).thenReturn(100);
        when(jobProperties.retention()).thenReturn(retention);
        when(retention.interactionMaxAgeDays()).thenReturn(180L);
        when(retentionService.prune(Duration.ofDays(180), 100))
                .thenReturn(100)
                .thenReturn(100)
                .thenReturn(35);

        scheduler.run();

        verify(retentionService, times(3)).prune(Duration.ofDays(180), 100);
    }

    @Test
    void warnsWhenTheLastBatchAtTheCapIsFull(CapturedOutput output) {
        when(schedulingProperties.prune()).thenReturn(pruneJob);
        when(pruneJob.batchSize()).thenReturn(100);
        when(jobProperties.retention()).thenReturn(retention);
        when(retention.interactionMaxAgeDays()).thenReturn(180L);
        when(retentionService.prune(Duration.ofDays(180), 100)).thenReturn(100);

        scheduler.run();

        verify(retentionService, times(100)).prune(Duration.ofDays(180), 100);
        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(true));
        assertThat(output).contains("Interaction prune reached the 100 batch cap with rows possibly remaining");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 35})
    void doesNotWarnWhenTheLastBatchAtTheCapIsPartial(int finalBatchDeleted, CapturedOutput output) {
        when(schedulingProperties.prune()).thenReturn(pruneJob);
        when(pruneJob.batchSize()).thenReturn(100);
        when(jobProperties.retention()).thenReturn(retention);
        when(retention.interactionMaxAgeDays()).thenReturn(180L);
        AtomicInteger batches = new AtomicInteger();
        when(retentionService.prune(Duration.ofDays(180), 100))
                .thenAnswer(invocation -> batches.incrementAndGet() < 100 ? 100 : finalBatchDeleted);

        scheduler.run();

        verify(retentionService, times(100)).prune(Duration.ofDays(180), 100);
        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(true));
        assertThat(output).doesNotContain("batch cap with rows possibly remaining");
    }

    @Test
    void skipsPruneWhenBatchSizeIsZeroOrNegative() {
        when(schedulingProperties.prune()).thenReturn(pruneJob);
        when(pruneJob.batchSize()).thenReturn(0);

        scheduler.run();

        verify(retentionService, never()).prune(any(Duration.class), anyInt());
        // A misconfiguration that silently deletes nothing is a retention problem, so it reports as a
        // failed run rather than a successful one that happened to find nothing to do.
        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(false));
    }

    @Test
    void aMetricsFailureAfterSuccessfulPruneDoesNotEscapeTheScheduler(CapturedOutput output) {
        when(schedulingProperties.prune()).thenReturn(pruneJob);
        when(pruneJob.batchSize()).thenReturn(100);
        when(jobProperties.retention()).thenReturn(retention);
        when(retention.interactionMaxAgeDays()).thenReturn(180L);
        when(retentionService.prune(Duration.ofDays(180), 100)).thenReturn(42);
        doThrow(new IllegalStateException("metrics down"))
                .when(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(true));

        assertDoesNotThrow(scheduler::run);

        verify(retentionService).prune(Duration.ofDays(180), 100);
        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(true));
        verify(metrics, never()).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(false));
        assertThat(output).contains("Could not record recommendation scheduler metrics for interaction-prune")
                .doesNotContain("Interaction prune failed");
    }

    @Test
    void aMetricsFailureDoesNotPreventLoggingTheOriginalPruneFailure(CapturedOutput output) {
        when(schedulingProperties.prune()).thenReturn(pruneJob);
        when(pruneJob.batchSize()).thenReturn(100);
        when(jobProperties.retention()).thenReturn(retention);
        when(retention.interactionMaxAgeDays()).thenReturn(180L);
        when(retentionService.prune(Duration.ofDays(180), 100))
                .thenThrow(new IllegalStateException("original prune failure"));
        doThrow(new IllegalStateException("metrics down"))
                .when(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(false));

        assertDoesNotThrow(scheduler::run);

        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(false));
        verify(metrics, never()).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(true));
        assertThat(output).contains("Could not record recommendation scheduler metrics for interaction-prune",
                "Interaction prune failed", "original prune failure");
    }

    @Test
    void aFailureIsRecordedRatherThanOnlyLogged() {
        // The scheduler swallows the exception so one bad run does not kill the pool thread; the metric is
        // what makes a prune that has been failing for weeks visible instead of log-only.
        when(schedulingProperties.prune()).thenReturn(pruneJob);
        when(pruneJob.batchSize()).thenReturn(100);
        when(jobProperties.retention()).thenReturn(retention);
        when(retention.interactionMaxAgeDays()).thenReturn(180L);
        when(retentionService.prune(any(Duration.class), anyInt()))
                .thenThrow(new IllegalStateException("boom"));

        scheduler.run();

        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(false));
    }
}