package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AccountDeletionRequest {

    private final String requestId;
    private final String userId;
    private AccountDeletionStatus status;
    private final LocalDateTime requestedAt;
    private final LocalDateTime scheduledDeletionAt;
    private LocalDateTime canceledAt;

    public AccountDeletionRequest(
            String requestId,
            String userId,
            AccountDeletionStatus status,
            LocalDateTime requestedAt,
            LocalDateTime scheduledDeletionAt,
            LocalDateTime canceledAt) {
        this.requestId = requestId;
        this.userId = userId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.scheduledDeletionAt = scheduledDeletionAt;
        this.canceledAt = canceledAt;
    }

    public static AccountDeletionRequest createPending(String requestId, String userId, int graceDays) {
        LocalDateTime now = LocalDateTime.now();
        return new AccountDeletionRequest(
                requestId,
                userId,
                AccountDeletionStatus.PENDING,
                now,
                now.plusDays(graceDays),
                null);
    }

    public void cancel() {
        this.status = AccountDeletionStatus.CANCELLED;
        this.canceledAt = LocalDateTime.now();
    }
}



