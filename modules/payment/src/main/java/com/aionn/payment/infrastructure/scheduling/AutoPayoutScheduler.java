package com.aionn.payment.infrastructure.scheduling;

import com.aionn.payment.application.port.out.MerchantBalanceQueryPort;
import com.aionn.payment.infrastructure.config.properties.PaymentAutoPayoutProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment.auto-payout", name = "enabled", havingValue = "true")
public class AutoPayoutScheduler {

    private final MerchantBalanceQueryPort balanceQueryPort;
    private final AutoPayoutWorker worker;
    private final PaymentAutoPayoutProperties properties;

    @Scheduled(cron = "${payment.auto-payout.cron:0 0 2 * * *}")
    @SchedulerLock(name = "payment-auto-payout", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
    public void run() {
        try {
            List<MerchantBalanceQueryPort.EligibleBalance> candidates =
                    balanceQueryPort.findEligibleForAutoPayout(
                            properties.threshold(), properties.currency(), properties.batchSize());
            int created = 0;
            for (MerchantBalanceQueryPort.EligibleBalance c : candidates) {
                if (processSingleCandidate(c)) {
                    created++;
                }
            }
            if (created > 0) {
                log.info("Auto-payout: created {} payout request(s)", created);
            }
        } catch (Exception ex) {
            log.error("Auto-payout sweep failed", ex);
        }
    }

    private boolean processSingleCandidate(MerchantBalanceQueryPort.EligibleBalance c) {
        try {
            return worker.payoutOne(c);
        } catch (RuntimeException ex) {
            log.warn("Auto-payout failed for {}: {}", c.merchantId(), ex.getMessage());
            return false;
        }
    }
}
