package com.aionn.payment.infrastructure.scheduling;

import com.aionn.payment.application.port.out.MerchantBalanceQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoPayoutSchedulerTest {

    @Mock
    private MerchantBalanceQueryPort balanceQueryPort;
    @Mock
    private AutoPayoutWorker worker;

    @InjectMocks
    private AutoPayoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "threshold", BigDecimal.valueOf(100000));
        ReflectionTestUtils.setField(scheduler, "currency", "VND");
        ReflectionTestUtils.setField(scheduler, "batchSize", 50);
    }

    @Test
    void shouldRunAutoPayoutForEligibleCandidates() {
        MerchantBalanceQueryPort.EligibleBalance candidate =
                new MerchantBalanceQueryPort.EligibleBalance("merch-1", "VND", BigDecimal.valueOf(200000));

        when(balanceQueryPort.findEligibleForAutoPayout(any(), any(), anyInt()))
                .thenReturn(List.of(candidate));
        when(worker.payoutOne(candidate)).thenReturn(true);

        scheduler.run();

        verify(worker).payoutOne(candidate);
    }

    @Test
    void shouldHandleWorkerExceptionsGracefully() {
        MerchantBalanceQueryPort.EligibleBalance candidate =
                new MerchantBalanceQueryPort.EligibleBalance("merch-1", "VND", BigDecimal.valueOf(200000));

        when(balanceQueryPort.findEligibleForAutoPayout(any(), any(), anyInt()))
                .thenReturn(List.of(candidate));
        when(worker.payoutOne(candidate)).thenThrow(new RuntimeException("Balance error"));

        scheduler.run();

        verify(worker).payoutOne(candidate);
    }
}
