package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationSchedulingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Refreshes profiles that are stale relative to their owner's activity. The query decides who qualifies
 * (comparing last interaction against {@code refreshed_at}), so the batch rotates through the population.
 * The scan window starts narrow and widens only while it finds nobody, bounded by the profile lookback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "recommendation.scheduling.profile-refresh", name = "enabled", havingValue = "true")
public class ProfileRefreshScheduler {

    static final String JOB_NAME = "profile-refresh";

    /** Overlap factor on the initial scan window so a delayed run still covers the gap it left behind. */
    private static final int WINDOW_SAFETY_FACTOR = 3;

    /** Growth applied to the scan window while it keeps coming back empty. */
    private static final int WINDOW_WIDENING_FACTOR = 8;

    private final InteractionPersistencePort interactionRepository;
    private final ProfileRefreshWorker worker;
    private final RecommendationSchedulingProperties properties;
    private final RecommendationJobProperties jobProperties;
    private final RecommendationMetricsPort metrics;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${recommendation.scheduling.profile-refresh.delay-ms:900000}")
    @SchedulerLock(
            name = "recommendation-profile-refresh", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void run() {
        JobRunMetrics run = JobRunMetrics.start(metrics, JOB_NAME);
        try {
            RecommendationSchedulingProperties.Job job = properties.profileRefresh();
            List<String> userIds = staleUserIds(clock.instant(), job.batchSize());
            if (userIds.isEmpty()) {
                run.succeeded();
                return;
            }

            int failed = 0;
            for (String userId : userIds) {
                if (!worker.refreshOne(userId)) {
                    failed++;
                }
            }
            // Failures are otherwise invisible: the sweep returns normally and the worker only warns per user.
            if (failed > 0) {
                run.failed();
                log.warn("Refreshed {} of {} affinity profile(s); {} failed",
                        userIds.size() - failed, userIds.size(), failed);
            } else {
                run.succeeded();
                log.info("Refreshed {} affinity profile(s)", userIds.size());
            }
        } catch (Exception exception) {
            run.failed();
            log.error("Profile refresh sweep failed", exception);
        }
    }

    /** Widening is safe: the query excludes profiles already newer than their last interaction. */
    private List<String> staleUserIds(Instant now, int batchSize) {
        Duration window = Duration.ofMillis(properties.profileRefresh().delayMs())
                .multipliedBy(WINDOW_SAFETY_FACTOR);
        Duration widest = Duration.ofDays(jobProperties.profile().lookbackDays());
        if (window.compareTo(widest) > 0) {
            window = widest;
        }

        while (true) {
            List<String> userIds =
                    interactionRepository.findUserIdsWithInteractionsSince(now.minus(window), batchSize);
            if (!userIds.isEmpty() || window.compareTo(widest) >= 0) {
                return userIds;
            }
            window = window.multipliedBy(WINDOW_WIDENING_FACTOR);
            if (window.compareTo(widest) > 0) {
                window = widest;
            }
            log.debug("No stale profiles in the scan window; widening to {}", window);
        }
    }
}
