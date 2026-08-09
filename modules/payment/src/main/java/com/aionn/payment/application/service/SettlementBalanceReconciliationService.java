package com.aionn.payment.application.service;

import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.MerchantBalancePersistencePort.ReconciliationMismatch;
import com.aionn.payment.application.port.out.observability.PaymentMetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementBalanceReconciliationService {

    private final MerchantBalancePersistencePort balanceRepository;
    private final PaymentMetricsPort metrics;

    @Transactional(readOnly = true)
    public Result reconcile() {
        long total = balanceRepository.countBalances();
        List<ReconciliationMismatch> mismatches = balanceRepository.findReconciliationMismatches();
        long mismatched = mismatches.size();
        long matched = Math.max(0, total - mismatched);
        metrics.settlementReconciliation(matched, mismatched);
        return new Result(total, matched, mismatches);
    }

    public record Result(long total, long matched, List<ReconciliationMismatch> mismatches) {
    }
}
