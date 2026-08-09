package com.aionn.ordering.infrastructure.scheduling;

import com.aionn.ordering.application.port.out.CompensationTaskPort;
import com.aionn.ordering.application.service.CompensationRetryService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CompensationRetrySchedulerTest {

    private static final Instant NOW = Instant.parse("2026-08-05T10:00:00Z");

    private final CompensationTaskPort taskPort = mock(CompensationTaskPort.class);
    private final CompensationRetryService retryService = mock(CompensationRetryService.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final CompensationRetryScheduler scheduler =
            new CompensationRetryScheduler(taskPort, retryService, clock);

    @Test
    void retriesEveryDueTaskInClaimOrder() {
        when(taskPort.findRetryableIds(NOW, CompensationRetryService.MAX_ATTEMPTS, 100))
                .thenReturn(List.of("task-1", "task-2"));

        scheduler.run();

        var order = inOrder(retryService);
        order.verify(retryService).retry("task-1");
        order.verify(retryService).retry("task-2");
        verify(taskPort).findRetryableIds(NOW, CompensationRetryService.MAX_ATTEMPTS, 100);
    }

    @Test
    void doesNothingWhenNoTaskIsDue() {
        when(taskPort.findRetryableIds(NOW, CompensationRetryService.MAX_ATTEMPTS, 100))
                .thenReturn(List.of());

        scheduler.run();

        verifyNoInteractions(retryService);
    }
}
