package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.application.service.PopularityService;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static com.aionn.recommendation.infrastructure.scheduling.PopularityScheduler.JOB_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularitySchedulerTest {

    @Mock private PopularityService popularityService;
    @Mock private RecommendationMetricsPort metrics;

    private PopularityScheduler scheduler() {
        return new PopularityScheduler(popularityService, jobProperties(), metrics);
    }

    private static RecommendationJobProperties jobProperties() {
        return new RecommendationJobProperties(
                new RecommendationJobProperties.Profile(10, 180),
                new RecommendationJobProperties.Similarity(180, 2, 50),
                new RecommendationJobProperties.Popularity(45),
                new RecommendationJobProperties.Retention(180),
                new RecommendationJobProperties.Execution(60, 500));
    }

    @Test
    void theConfiguredLookbackReachesTheRebuild() {
        when(popularityService.refresh(any())).thenReturn(12);

        scheduler().run();

        verify(popularityService).refresh(Duration.ofDays(45));
    }

    @Test
    void aSuccessfulRebuildIsRecorded() {
        when(popularityService.refresh(any())).thenReturn(12);

        scheduler().run();

        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(true));
    }

    @Test
    void aFailedRebuildIsRecordedRatherThanOnlyLogged() {
        // Popularity is the fallback every other surface lands on, so a rebuild that has been failing
        // quietly degrades the whole module rather than one endpoint.
        when(popularityService.refresh(any())).thenThrow(new IllegalStateException("boom"));

        scheduler().run();

        verify(metrics).recordSchedulerExecution(eq(JOB_NAME), anyLong(), eq(false));
    }
}
