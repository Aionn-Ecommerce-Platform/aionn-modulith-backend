package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

class AccountDeletionRequestTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void constructor_validInput_createsInstance() {
        Instant now = Instant.now(Clock.systemUTC());
        Instant scheduled = now.plus(Duration.ofDays(30));

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
        AccountDeletionRequest request = AccountDeletionRequest.createPending("del-req-789", "user-123", 30, CLOCK);

        assertThat(request.getRequestId()).isEqualTo("del-req-789");
        assertThat(request.getUserId()).isEqualTo("user-123");
        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.PENDING);
        assertThat(request.getRequestedAt()).isEqualTo(NOW);
        assertThat(request.getScheduledDeletionAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(request.getCanceledAt()).isNull();
    }

    @Test
    void createPending_withCustomGracePeriod_schedulesCorrectly() {
        AccountDeletionRequest request = AccountDeletionRequest.createPending("del-req-999", "user-999", 7, CLOCK);

        assertThat(request.getScheduledDeletionAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void cancel_pendingRequest_cancelRequestAndSetsTimestamp() {
        AccountDeletionRequest request = AccountDeletionRequest.createPending("del-req-555", "user-555", 30, CLOCK);
        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.PENDING);
        assertThat(request.getCanceledAt()).isNull();

        request.cancel(CLOCK);

        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.CANCELLED);
        assertThat(request.getCanceledAt()).isEqualTo(NOW);
    }

    @Test
    void cancel_alreadyCanceled_updatesCanceledAt() {
        AccountDeletionRequest request = AccountDeletionRequest.createPending("del-req-777", "user-777", 30, CLOCK);
        request.cancel(CLOCK);
        Instant firstCancellation = request.getCanceledAt();

        Clock laterClock = Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC);
        request.cancel(laterClock);

        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.CANCELLED);
        assertThat(request.getCanceledAt()).isAfter(firstCancellation);
    }
}
