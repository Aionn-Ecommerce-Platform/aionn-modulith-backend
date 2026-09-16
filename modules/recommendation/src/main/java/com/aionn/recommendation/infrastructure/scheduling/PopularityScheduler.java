package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.application.service.PopularityService;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "recommendation.scheduling.popularity", name = "enabled", havingValue = "true")
public class PopularityScheduler {

    static final String JOB_NAME = "popularity";

    private final PopularityService popularityService;
    private final RecommendationJobProperties properties;
    private final RecommendationMetricsPort metrics;

    @Scheduled(fixedDelayString = "${recommendation.scheduling.popularity.delay-ms:900000}")
    @SchedulerLock(name = "recommendation-popularity", lockAtMostFor = "PT30M", lockAtLeastFor = "PT30S")
    public void run() {
        JobRunMetrics run = JobRunMetrics.start(metrics, JOB_NAME);
        try {
            popularityService.refresh(Duration.ofDays(properties.popularity().lookbackDays()));
            run.succeeded();
        } catch (Exception exception) {
            run.failed();
            // The rebuild commits in batches, so a failure part-way leaves the table holding a mix of
            // new and previous scores rather than the previous scores intact. That is tolerable because
            // every row upserts on its product ID and the next run recomputes the whole window, but the
            // lock has to outlast the configured compute timeout or a slow run loses the lock mid-write
            // and a second instance starts the same rebuild against it.
            log.error("Popularity refresh failed", exception);
        }
    }
}
