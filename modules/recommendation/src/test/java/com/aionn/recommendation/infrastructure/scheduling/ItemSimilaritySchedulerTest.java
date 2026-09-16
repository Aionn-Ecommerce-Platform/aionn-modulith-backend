package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.application.service.ItemSimilarityService;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static com.aionn.recommendation.infrastructure.scheduling.ItemSimilarityScheduler.JOB_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemSimilaritySchedulerTest {

    @Mock private ItemSimilarityService itemSimilarityService;
    @Mock private RecommendationMetricsPort metrics;

    private ItemSimilarityScheduler scheduler() {
        return new ItemSimilarityScheduler(itemSimilarityService, jobProperties(), metrics);
    }

    private static RecommendationJobProperties jobProperties() {
        return new RecommendationJobProperties(
                new RecommendationJobProperties.Profile(10, 180),
                new RecommendationJobProperties.Similarity(180, 3, 25),
                new RecommendationJobProperties.Popularity(30),
                new RecommendationJobProperties.Retention(180),
                new RecommendationJobProperties.Execution(60, 500));
    }

    @Test
    void everyConfiguredKnobReachesTheRebuild() {
        // The lookback, the co-occurrence floor and the neighbour cap are the three things an operator
        // tunes to trade matrix size against signal quality; a scheduler that drops one of them makes the
        // setting silently inert.
        when(itemSimilarityService.refresh(any(), anyInt(), anyInt())).thenReturn(4);

        scheduler().run();

        verify(itemSimilarityService).refresh(Duration.ofDays(180), 3, 25);
    }

    @Test
    void aSuccessfulRebuildIsRecorded() {
        when(itemSimilarityService.refresh(any(), anyInt(), anyInt())).thenReturn(4);

        scheduler().run();

        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(true));
    }

    @Test
    void aFailedRebuildIsRecordedRatherThanOnlyLogged() {
        // The scheduler swallows the exception so one bad run cannot kill the pool thread. Without the
        // metric the matrix stays empty and every signed-in home feed falls back to trending, which looks
        // identical to a catalogue with no co-purchase data yet.
        when(itemSimilarityService.refresh(any(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("boom"));

        scheduler().run();

        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(false));
    }

    @Test
    void nothingIsWrittenWhenTheRebuildThrows() {
        when(itemSimilarityService.refresh(any(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("boom"));

        scheduler().run();

        verify(metrics, never()).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(true));
    }
}
