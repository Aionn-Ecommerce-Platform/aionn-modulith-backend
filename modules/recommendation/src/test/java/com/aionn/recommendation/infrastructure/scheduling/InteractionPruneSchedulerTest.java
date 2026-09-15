package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.application.service.InteractionRetentionService;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationSchedulingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static com.aionn.recommendation.infrastructure.scheduling.InteractionPruneScheduler.JOB_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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