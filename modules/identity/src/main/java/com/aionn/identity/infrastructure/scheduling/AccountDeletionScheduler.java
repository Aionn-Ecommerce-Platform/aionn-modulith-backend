package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountDeletionScheduler {

    private final AccountDeletionProcessor processor;
    private final RefreshTokenStorePort refreshTokenStore;

    @Scheduled(cron = "0 23 3 * * *")
    @SchedulerLock(name = "identity-account-deletion", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void completeDueRequests() {
        int completed = 0;
        AccountDeletionProcessor.Result result;
        do {
            result = processor.completeDueRequests();
            completed += result.completedAccounts();
            result.revokedSessionIds().forEach(this::revokeRefreshTokensSafely);
        } while (result.completedAccounts() == AccountDeletionProcessor.BATCH_SIZE);
        if (completed > 0) {
            log.info("Completed {} account deletion requests", completed);
        }
    }

    private void revokeRefreshTokensSafely(String sessionId) {
        try {
            refreshTokenStore.revokeBySessionId(sessionId);
        } catch (RuntimeException ex) {
            log.warn("Failed to remove refresh tokens for a revoked account session", ex);
        }
    }
}
