package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.infrastructure.config.properties.AuthSessionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;

/**
 * Runs daily at 03:17 server time, off the round hour to avoid clashing with other jobs.
 * Deletes auth sessions that have been idle longer than the configured retention period.
 * ShedLock keeps the purge single-instance-safe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthSessionCleanupScheduler {

    private final AuthSessionPersistencePort authSessionPersistence;
    private final AuthSessionProperties properties;
    private final Clock clock;

    @Scheduled(cron = "0 17 3 * * *")
    @SchedulerLock(name = "auth-session-cleanup", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void purgeIdleSessions() {
        Instant cutoff = clock.instant().minus(Duration.ofDays(properties.retentionDays()));
        int deleted = authSessionPersistence.deleteIdleBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} auth sessions idle since before {}", deleted, cutoff);
        }
    }
}
