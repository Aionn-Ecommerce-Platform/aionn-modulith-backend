package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
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
 *
 * <p>Because the retention obligation is the point, a failure here is recorded as a metric rather than
 * only logged: a prune that has been failing for weeks is a data-retention problem, not a performance
 * one.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "recommendation.scheduling.prune", name = "enabled", havingValue = "true")
public class InteractionPruneScheduler {

    static final String JOB_NAME = "interaction-prune";

    private final InteractionRetentionService retentionService;
    private final RecommendationJobProperties jobProperties;
    private final RecommendationSchedulingProperties schedulingProperties;
    private final RecommendationMetricsPort metrics;

    private static final int MAX_BATCHES_PER_RUN = 100;

    @Scheduled(fixedDelayString = "${recommendation.scheduling.prune.delay-ms:86400000}")
    @SchedulerLock(name = "recommendation-interaction-prune", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
    public void run() {
        JobRunMetrics run = JobRunMetrics.start(metrics, JOB_NAME);
        try {
            int batchSize = schedulingProperties.prune().batchSize();
            if (batchSize <= 0) {
                log.warn("Interaction prune skipped: batchSize must be greater than 0, got {}", batchSize);
                run.failed();
                return;
            }
            Duration maxAge = Duration.ofDays(jobProperties.retention().interactionMaxAgeDays());
            int totalDeleted = 0;
            int batches = 0;
            int deleted;
            do {
                deleted = retentionService.prune(maxAge, batchSize);
                totalDeleted += deleted;
                batches++;
            } while (deleted >= batchSize && batches < MAX_BATCHES_PER_RUN);

            run.succeeded();
            if (totalDeleted > 0) {
                log.info("Pruned {} total interaction(s) across {} batch(es)", totalDeleted, batches);
            }
            if (batches >= MAX_BATCHES_PER_RUN) {
                // More rows aged out than one run is allowed to delete. Not an error, but the backlog
                // grows if this repeats, so it is worth seeing.
                log.warn("Interaction prune reached the {} batch cap with rows still to delete",
                        MAX_BATCHES_PER_RUN);
            }
        } catch (Exception exception) {
            run.failed();
            log.error("Interaction prune failed", exception);
        }
    }
}
