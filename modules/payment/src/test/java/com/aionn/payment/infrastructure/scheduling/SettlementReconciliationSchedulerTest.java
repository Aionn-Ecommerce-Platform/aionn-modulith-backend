package com.aionn.payment.infrastructure.scheduling;

import com.aionn.payment.application.port.out.MerchantBalancePersistencePort.ReconciliationMismatch;
import com.aionn.payment.application.service.SettlementBalanceReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementReconciliationSchedulerTest {

    @Mock private SettlementBalanceReconciliationService service;

    @Test
    void delegatesScheduledReconciliation() {
        var mismatch = new ReconciliationMismatch(
                "merchant-1", "VND",
                BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        when(service.reconcile()).thenReturn(
                new SettlementBalanceReconciliationService.Result(1, 0, List.of(mismatch)));

        new SettlementReconciliationScheduler(service).reconcileBalances();

        verify(service).reconcile();
    }
}
