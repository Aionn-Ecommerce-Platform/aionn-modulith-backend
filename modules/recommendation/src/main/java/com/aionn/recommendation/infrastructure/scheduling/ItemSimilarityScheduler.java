package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.service.ItemSimilarityService;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Rebuilds the similarity matrix hourly.
 *
 * <p>{@code lockAtMostFor} is generous because the co-occurrence self-join grows with the square of the
 * average basket size. Measure the real worst case before tightening it: a lock that expires mid-run
 * lets a second instance start the same rebuild.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "recommendation.scheduling.item-similarity", name = "enabled", havingValue = "true")
public class ItemSimilarityScheduler {

    private final ItemSimilarityService itemSimilarityService;
    private final RecommendationJobProperties properties;

    @Scheduled(fixedDelayString = "${recommendation.scheduling.item-similarity.delay-ms:3600000}")
    @SchedulerLock(
            name = "recommendation-item-similarity", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void run() {
        try {
            itemSimilarityService.refresh(
                    Duration.ofDays(properties.similarity().lookbackDays()),
                    properties.similarity().minCoOccurrence());
        } catch (Exception exception) {
            log.error("Item similarity refresh failed", exception);
        }
    }
}
