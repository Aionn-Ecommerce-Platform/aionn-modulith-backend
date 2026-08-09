package com.aionn.ordering.application.service;

import com.aionn.ordering.application.port.out.CompensationTaskPort;
import com.aionn.ordering.application.port.out.CompensationTaskPort.Task;
import com.aionn.ordering.application.port.out.CompensationTaskPort.Type;
import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.VoucherGateway;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CompensationRetryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-05T10:00:00Z");

    private final CompensationTaskPort taskPort = mock(CompensationTaskPort.class);
    private final StockReservationGateway stockGateway = mock(StockReservationGateway.class);
    private final VoucherGateway voucherGateway = mock(VoucherGateway.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final CompensationRetryService service =
            new CompensationRetryService(taskPort, stockGateway, voucherGateway, clock);

    @Test
    void ignoresATaskThatIsNoLongerPending() {
        when(taskPort.findById("missing")).thenReturn(Optional.empty());

        service.retry("missing");

        verifyNoInteractions(stockGateway, voucherGateway);
        verify(taskPort, never()).markCompleted(anyString());
        verify(taskPort, never()).markFailed(anyString(), anyString(), any(), anyBoolean());
    }

    @Test
    void releasesTheReservationAndCompletesTheTask() {
        when(taskPort.findById("task-1")).thenReturn(Optional.of(new Task(
                "task-1", Type.RESERVATION_RELEASE, "res-1", "user-1", "order-1", "placement-aborted", 0)));

        service.retry("task-1");

        verify(stockGateway).release("res-1", "placement-aborted");
        verify(taskPort).markCompleted("task-1");
        verifyNoInteractions(voucherGateway);
    }

    @Test
    void releasesTheVoucherAndCompletesTheTask() {
        when(taskPort.findById("task-2")).thenReturn(Optional.of(new Task(
                "task-2", Type.VOUCHER_RELEASE, "voucher-1", "user-1", "order-1", "placement-aborted", 0)));

        service.retry("task-2");

        verify(voucherGateway).release("user-1", "order-1", "placement-aborted");
        verify(taskPort).markCompleted("task-2");
        verifyNoInteractions(stockGateway);
    }

    @Test
    void recordsAFailureWithExponentialBackoffAndLeavesTheTaskRetryable() {
        when(taskPort.findById("task-3")).thenReturn(Optional.of(new Task(
                "task-3", Type.RESERVATION_RELEASE, "res-1", "user-1", "order-1", "aborted", 2)));
        doThrow(new IllegalStateException("inventory unavailable"))
                .when(stockGateway).release("res-1", "aborted");

        service.retry("task-3");

        // attempt 3 -> 1 << 3 == 8 seconds of backoff, still short of MAX_ATTEMPTS
        verify(taskPort).markFailed("task-3", "inventory unavailable", NOW.plusSeconds(8), false);
        verify(taskPort, never()).markCompleted(anyString());
    }

    @Test
    void deadLettersTheTaskOnTheFinalAttempt() {
        when(taskPort.findById("task-4")).thenReturn(Optional.of(new Task(
                "task-4", Type.VOUCHER_RELEASE, "voucher-1", "user-1", "order-1", "aborted",
                CompensationRetryService.MAX_ATTEMPTS - 1)));
        doThrow(new IllegalStateException("promotion unavailable"))
                .when(voucherGateway).release("user-1", "order-1", "aborted");

        service.retry("task-4");

        verify(taskPort).markFailed("task-4", "promotion unavailable", NOW.plusSeconds(1024), true);
    }

    @Test
    void capsTheBackoffDelayForAHighAttemptCount() {
        when(taskPort.findById("task-5")).thenReturn(Optional.of(new Task(
                "task-5", Type.RESERVATION_RELEASE, "res-1", "user-1", "order-1", "aborted", 20)));
        doThrow(new IllegalStateException("still failing"))
                .when(stockGateway).release("res-1", "aborted");

        service.retry("task-5");

        // 1 << min(21, 12) == 4096, clamped to the 3600 second ceiling
        verify(taskPort).markFailed("task-5", "still failing", NOW.plusSeconds(3600), true);
    }
}
