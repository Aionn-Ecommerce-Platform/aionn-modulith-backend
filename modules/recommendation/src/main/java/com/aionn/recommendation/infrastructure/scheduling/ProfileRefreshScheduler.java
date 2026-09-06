package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
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
 * Refreshes the profiles of users who have acted since the last sweep.
 *
 * <p>Incremental rather than full-table: profiles only change when their owner interacts, so rescanning
 * every user would waste most of its work. The lookback window is derived from the schedule interval
 * with a margin, so a slow or skipped run does not silently drop users.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "recommendation.scheduling.profile-refresh", name = "enabled", havingValue = "true")
public class ProfileRefreshScheduler {

    /** Overlap factor on the scan window so a delayed run still covers the gap it left behind. */
    private static final int WINDOW_SAFETY_FACTOR = 3;

    private final InteractionPersistencePort interactionRepository;
    private final ProfileRefreshWorker worker;
    private final RecommendationSchedulingProperties properties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${recommendation.scheduling.profile-refresh.delay-ms:900000}")
    @SchedulerLock(
            name = "recommendation-profile-refresh", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void run() {
        try {
            RecommendationSchedulingProperties.Job job = properties.profileRefresh();
            Instant since = clock.instant()
                    .minus(Duration.ofMillis(job.delayMs()).multipliedBy(WINDOW_SAFETY_FACTOR));

            List<String> userIds = interactionRepository.findUserIdsWithInteractionsSince(
                    since, job.batchSize());
            if (userIds.isEmpty()) {
                return;
            }

            int refreshed = 0;
            for (String userId : userIds) {
                if (worker.refreshOne(userId)) {
                    refreshed++;
                }
            }
            log.info("Refreshed {} of {} affinity profile(s)", refreshed, userIds.size());
        } catch (Exception exception) {
            log.error("Profile refresh sweep failed", exception);
        }
    }
}
