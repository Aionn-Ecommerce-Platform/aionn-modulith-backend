package com.aionn.payment.infrastructure.scheduling;

import com.aionn.payment.application.service.SettlementBalanceReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementReconciliationScheduler {

    private final SettlementBalanceReconciliationService service;

    @Scheduled(cron = "0 11 4 * * *")
    @SchedulerLock(name = "payment-settlement-reconciliation", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void reconcileBalances() {
        var result = service.reconcile();
        if (!result.mismatches().isEmpty()) {
            log.error("Settlement reconciliation found {} mismatched merchant balance buckets out of {}",
                    result.mismatches().size(), result.total());
        }
    }
}
