package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.service.InteractionRetentionService;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationSchedulingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Ages out old interactions daily.
 *
 * <p>Two reasons this is not optional: the log is append-only and grows without bound, and it holds
 * personal behavioural data that should not be retained indefinitely. Decay already makes very old rows
 * numerically irrelevant, so deleting them changes recommendations imperceptibly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "recommendation.scheduling.prune", name = "enabled", havingValue = "true")
public class InteractionPruneScheduler {

    private final InteractionRetentionService retentionService;
    private final RecommendationJobProperties jobProperties;
    private final RecommendationSchedulingProperties schedulingProperties;

    @Scheduled(fixedDelayString = "${recommendation.scheduling.prune.delay-ms:86400000}")
    @SchedulerLock(
            name = "recommendation-interaction-prune", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
    public void run() {
        try {
            retentionService.prune(
                    Duration.ofDays(jobProperties.retention().interactionMaxAgeDays()),
                    schedulingProperties.prune().batchSize());
        } catch (Exception exception) {
            log.error("Interaction prune failed", exception);
        }
    }
}
