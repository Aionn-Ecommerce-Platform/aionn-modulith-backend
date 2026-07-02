package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class AccountDeletionRequestTest {

    @Test
    void constructor_validInput_createsInstance() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        LocalDateTime scheduled = now.plusDays(30);

        AccountDeletionRequest request = new AccountDeletionRequest(
                "del-req-123",
                "user-456",
                AccountDeletionStatus.PENDING,
                now,
                scheduled,
                null);

        assertThat(request.getRequestId()).isEqualTo("del-req-123");
        assertThat(request.getUserId()).isEqualTo("user-456");
        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.PENDING);
        assertThat(request.getRequestedAt()).isEqualTo(now);
        assertThat(request.getScheduledDeletionAt()).isEqualTo(scheduled);
        assertThat(request.getCanceledAt()).isNull();
    }

    @Test
    void createPending_validInput_createsPendingRequest() {
        AccountDeletionRequest request = AccountDeletionRequest.createPending("del-req-789", "user-123", 30);

        assertThat(request.getRequestId()).isEqualTo("del-req-789");
        assertThat(request.getUserId()).isEqualTo("user-123");
        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.PENDING);
        assertThat(request.getRequestedAt()).isNotNull();
        assertThat(request.getRequestedAt()).isBeforeOrEqualTo(LocalDateTime.now(Clock.systemUTC()));
        assertThat(request.getScheduledDeletionAt()).isNotNull();
        assertThat(request.getScheduledDeletionAt())
                .isAfterOrEqualTo(request.getRequestedAt().plusDays(29))
                .isBeforeOrEqualTo(request.getRequestedAt().plusDays(31));
        assertThat(request.getCanceledAt()).isNull();
    }

    @Test
    void createPending_withCustomGracePeriod_schedulesCorrectly() {
        AccountDeletionRequest request = AccountDeletionRequest.createPending("del-req-999", "user-999", 7);

        assertThat(request.getScheduledDeletionAt())
                .isAfterOrEqualTo(request.getRequestedAt().plusDays(6))
                .isBeforeOrEqualTo(request.getRequestedAt().plusDays(8));
    }

    @Test
    void cancel_pendingRequest_cancelRequestAndSetsTimestamp() {
        AccountDeletionRequest request = AccountDeletionRequest.createPending("del-req-555", "user-555", 30);
        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.PENDING);
        assertThat(request.getCanceledAt()).isNull();

        request.cancel();

        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.CANCELLED);
        assertThat(request.getCanceledAt()).isNotNull();
        assertThat(request.getCanceledAt()).isBeforeOrEqualTo(LocalDateTime.now(Clock.systemUTC()));
    }

    @Test
    void cancel_alreadyCanceled_updatesCanceledAt() {
        AccountDeletionRequest request = AccountDeletionRequest.createPending("del-req-777", "user-777", 30);
        request.cancel();
        LocalDateTime firstCancellation = request.getCanceledAt();

        request.cancel();

        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.CANCELLED);
        assertThat(request.getCanceledAt()).isAfterOrEqualTo(firstCancellation);
    }
}
