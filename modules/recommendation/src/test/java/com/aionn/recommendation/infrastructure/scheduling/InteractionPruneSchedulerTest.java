package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.service.InteractionRetentionService;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationSchedulingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionPruneSchedulerTest {

    @Mock private InteractionRetentionService retentionService;
    @Mock private RecommendationJobProperties jobProperties;
    @Mock private RecommendationSchedulingProperties schedulingProperties;
    @Mock private RecommendationJobProperties.Retention retention;
    @Mock private RecommendationSchedulingProperties.Job pruneJob;

    private InteractionPruneScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(jobProperties.retention()).thenReturn(retention);
        when(retention.interactionMaxAgeDays()).thenReturn(180L);
        when(schedulingProperties.prune()).thenReturn(pruneJob);
        when(pruneJob.batchSize()).thenReturn(100);

        scheduler = new InteractionPruneScheduler(retentionService, jobProperties, schedulingProperties);
    }

    @Test
    void runsSingleBatchWhenDeletedIsLessThanBatchSize() {
        when(retentionService.prune(eq(Duration.ofDays(180)), eq(100))).thenReturn(42);

        scheduler.run();

        verify(retentionService, times(1)).prune(Duration.ofDays(180), 100);
    }

    @Test
    void loopsUntilExpiredRowsAreDrained() {
        when(retentionService.prune(eq(Duration.ofDays(180)), eq(100)))
                .thenReturn(100)
                .thenReturn(100)
                .thenReturn(35);

        scheduler.run();

        verify(retentionService, times(3)).prune(Duration.ofDays(180), 100);
    }
}