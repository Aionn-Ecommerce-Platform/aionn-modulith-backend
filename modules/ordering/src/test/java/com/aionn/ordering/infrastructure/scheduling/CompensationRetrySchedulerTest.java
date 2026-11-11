package com.aionn.ordering.infrastructure.scheduling;

import com.aionn.ordering.application.port.out.CompensationTaskPort;
import com.aionn.ordering.application.policy.CompensationRetryPolicy;
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
    private static final int MAX_ATTEMPTS = 10;
    private static final int BATCH_SIZE = 100;

    private final CompensationTaskPort taskPort = mock(CompensationTaskPort.class);
    private final CompensationRetryService retryService = mock(CompensationRetryService.class);
    private final CompensationRetryPolicy retryPolicy = mock(CompensationRetryPolicy.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final CompensationRetryScheduler scheduler =
            new CompensationRetryScheduler(taskPort, retryService, retryPolicy, clock);

    {
        when(retryPolicy.maxAttempts()).thenReturn(MAX_ATTEMPTS);
        when(retryPolicy.batchSize()).thenReturn(BATCH_SIZE);
    }

    @Test
    void retriesEveryDueTaskInClaimOrder() {
        when(taskPort.findRetryableIds(NOW, MAX_ATTEMPTS, BATCH_SIZE))
                .thenReturn(List.of("task-1", "task-2"));

        scheduler.run();

        var order = inOrder(retryService);
        order.verify(retryService).retry("task-1");
        order.verify(retryService).retry("task-2");
        verify(taskPort).findRetryableIds(NOW, MAX_ATTEMPTS, BATCH_SIZE);
    }

    @Test
    void doesNothingWhenNoTaskIsDue() {
        when(taskPort.findRetryableIds(NOW, MAX_ATTEMPTS, BATCH_SIZE))
                .thenReturn(List.of());

        scheduler.run();

        verifyNoInteractions(retryService);
    }
}
