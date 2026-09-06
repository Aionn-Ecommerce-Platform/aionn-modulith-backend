package com.aionn.recommendation.infrastructure.scheduling;

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

    private final PopularityService popularityService;
    private final RecommendationJobProperties properties;

    @Scheduled(fixedDelayString = "${recommendation.scheduling.popularity.delay-ms:900000}")
    @SchedulerLock(name = "recommendation-popularity", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void run() {
        try {
            popularityService.refresh(Duration.ofDays(properties.popularity().lookbackDays()));
        } catch (Exception exception) {
            // A failed sweep leaves the previous scores in place; the next run recomputes from scratch,
            // so there is nothing to compensate.
            log.error("Popularity refresh failed", exception);
        }
    }
}
