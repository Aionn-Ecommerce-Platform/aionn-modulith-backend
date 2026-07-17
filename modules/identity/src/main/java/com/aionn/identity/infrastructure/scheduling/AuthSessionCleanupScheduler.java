package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthSessionCleanupScheduler {

    private static final Duration RETENTION = Duration.ofDays(90);

    private final AuthSessionPersistencePort authSessionPersistence;
    private final Clock clock;

    // Daily at 03:17 local time — off-peak, off the round hour mark.
    @Scheduled(cron = "0 17 3 * * *")
    public void purgeIdleSessions() {
        Instant cutoff = Instant.now(clock).minus(RETENTION);
        int deleted = authSessionPersistence.deleteIdleBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} auth sessions idle since before {}", deleted, cutoff);
        }
    }
}
