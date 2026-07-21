package com.aionn.payment.infrastructure.scheduling;

import com.aionn.payment.application.port.out.MerchantBalanceQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoPayoutSchedulerTest {

    @Mock
    private MerchantBalanceQueryPort balanceQueryPort;

    @Mock
    private AutoPayoutWorker worker;

    @InjectMocks
    private AutoPayoutScheduler scheduler;

    @Test
    void runProcessesEligibleCandidatesSuccessfully() {
        ReflectionTestUtils.setField(scheduler, "threshold", BigDecimal.valueOf(100000));
        ReflectionTestUtils.setField(scheduler, "currency", "VND");
        ReflectionTestUtils.setField(scheduler, "batchSize", 50);

        MerchantBalanceQueryPort.EligibleBalance candidate =
                new MerchantBalanceQueryPort.EligibleBalance("m-1", "VND", BigDecimal.valueOf(500000));

        when(balanceQueryPort.findEligibleForAutoPayout(BigDecimal.valueOf(100000), "VND", 50))
                .thenReturn(List.of(candidate));
        when(worker.payoutOne(candidate)).thenReturn(true);

        scheduler.run();

        verify(worker).payoutOne(candidate);
    }

    @Test
    void runHandlesWorkerExceptionGracefully() {
        ReflectionTestUtils.setField(scheduler, "threshold", BigDecimal.valueOf(100000));
        ReflectionTestUtils.setField(scheduler, "currency", "VND");
        ReflectionTestUtils.setField(scheduler, "batchSize", 50);

        MerchantBalanceQueryPort.EligibleBalance candidate =
                new MerchantBalanceQueryPort.EligibleBalance("m-1", "VND", BigDecimal.valueOf(500000));

        when(balanceQueryPort.findEligibleForAutoPayout(BigDecimal.valueOf(100000), "VND", 50))
                .thenReturn(List.of(candidate));
        doThrow(new RuntimeException("Payout failed")).when(worker).payoutOne(candidate);

        scheduler.run();

        verify(worker).payoutOne(candidate);
    }
}
