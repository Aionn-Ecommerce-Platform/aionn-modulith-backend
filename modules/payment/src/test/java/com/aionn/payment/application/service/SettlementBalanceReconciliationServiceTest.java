package com.aionn.payment.application.service;

import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.MerchantBalancePersistencePort.ReconciliationMismatch;
import com.aionn.payment.application.port.out.observability.PaymentMetricsPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementBalanceReconciliationServiceTest {

    @Mock private MerchantBalancePersistencePort balanceRepository;
    @Mock private PaymentMetricsPort metrics;

    @Test
    void reportsMatchedAndMismatchedBalanceBuckets() {
        ReconciliationMismatch mismatch = new ReconciliationMismatch(
                "merchant-1", "VND",
                BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        when(balanceRepository.countBalances()).thenReturn(3L);
        when(balanceRepository.findReconciliationMismatches()).thenReturn(List.of(mismatch));
        var service = new SettlementBalanceReconciliationService(balanceRepository, metrics);

        var result = service.reconcile();

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.matched()).isEqualTo(2);
        assertThat(result.mismatches()).containsExactly(mismatch);
        verify(metrics).settlementReconciliation(2, 1);
    }
}
