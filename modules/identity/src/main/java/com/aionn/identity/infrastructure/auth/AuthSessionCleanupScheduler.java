package com.aionn.identity.infrastructure.auth;

import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Removes auth_sessions rows that have been idle for over 90 days.
 *
 * <p>Backend creates a new session row on every login and refresh-token
 * rotation by design (audit trail + reuse-detection). Without cleanup the
 * table grows unbounded; under load it bloats the joins on the security page
 * and slows session lookups. 90 days is well past any reasonable refresh
 * window, so anything older is dead data.
 *
 * <p>Hard delete (not soft) since this is operational data — once a session
 * is past its useful audit window, keeping it adds no value.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthSessionCleanupScheduler {

    private static final Duration RETENTION = Duration.ofDays(90);

    private final AuthSessionPersistencePort authSessionPersistence;

    // Daily at 03:17 local time — off-peak, off the round hour mark.
    @Scheduled(cron = "0 17 3 * * *")
    public void purgeIdleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minus(RETENTION);
        int deleted = authSessionPersistence.deleteIdleBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} auth sessions idle since before {}", deleted, cutoff);
        }
    }
}
