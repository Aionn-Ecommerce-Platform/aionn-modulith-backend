package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;

@Getter
public class AccountDeletionRequest {

    private final String requestId;
    private final String userId;
    private AccountDeletionStatus status;
    private final Instant requestedAt;
    private final Instant scheduledDeletionAt;
    private Instant canceledAt;

    public AccountDeletionRequest(
            String requestId,
            String userId,
            AccountDeletionStatus status,
            Instant requestedAt,
            Instant scheduledDeletionAt,
            Instant canceledAt) {
        this.requestId = requestId;
        this.userId = userId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.scheduledDeletionAt = scheduledDeletionAt;
        this.canceledAt = canceledAt;
    }

    public static AccountDeletionRequest createPending(String requestId, String userId, int graceDays) {
        return createPending(requestId, userId, graceDays, Clock.systemUTC());
    }

    public static AccountDeletionRequest createPending(String requestId, String userId, int graceDays, Clock clock) {
        Instant now = clock.instant();
        return new AccountDeletionRequest(
                requestId,
                userId,
                AccountDeletionStatus.PENDING,
                now,
                now.plus(Duration.ofDays(graceDays)),
                null);
    }

    public void cancel() {
        cancel(Clock.systemUTC());
    }

    public void cancel(Clock clock) {
        this.status = AccountDeletionStatus.CANCELLED;
        this.canceledAt = clock.instant();
    }
}



