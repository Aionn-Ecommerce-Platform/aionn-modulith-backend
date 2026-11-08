package com.aionn.ucp.infrastructure.scheduling;

import com.aionn.ucp.infrastructure.persistence.JpaUcpCheckoutSessionAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reclaims expired UCP checkout session rows (reads already treat them as absent). Disabled by default
 * because the durable adapter may not be wired in test contexts; ShedLock keeps it to one instance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aionn.ucp.session-cleanup", name = "enabled", havingValue = "true")
public class UcpCheckoutSessionCleanupScheduler {

    private final JpaUcpCheckoutSessionAdapter sessionAdapter;

    @Scheduled(fixedDelayString = "${aionn.ucp.session-cleanup.delay-ms:3600000}")
    @SchedulerLock(name = "ucp-checkout-session-cleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void run() {
        try {
            int removed = sessionAdapter.evictExpired();
            if (removed > 0) {
                log.info("UCP checkout session cleanup removed {} expired sessions", removed);
            }
        } catch (RuntimeException ex) {
            // Non-fatal: the next tick retries; don't mark the scheduler thread failed for a DB hiccup.
            log.warn("UCP checkout session cleanup failed", ex);
        }
    }
}
