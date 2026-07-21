package com.aionn.payment.infrastructure.scheduling;

import com.aionn.payment.application.port.out.MerchantBalanceQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment.auto-payout", name = "enabled", havingValue = "true")
public class AutoPayoutScheduler {

    private final MerchantBalanceQueryPort balanceQueryPort;
    private final AutoPayoutWorker worker;

    @Value("${payment.auto-payout.threshold:100000}")
    private BigDecimal threshold;

    @Value("${payment.auto-payout.currency:VND}")
    private String currency;

    @Value("${payment.auto-payout.batch-size:50}")
    private int batchSize;

    @Scheduled(cron = "${payment.auto-payout.cron:0 0 2 * * *}")
    public void run() {
        try {
            List<MerchantBalanceQueryPort.EligibleBalance> candidates =
                    balanceQueryPort.findEligibleForAutoPayout(threshold, currency, batchSize);
            int created = 0;
            for (MerchantBalanceQueryPort.EligibleBalance c : candidates) {
                try {
                    if (worker.payoutOne(c)) created++;
                } catch (RuntimeException ex) {
                    log.warn("Auto-payout failed for {}: {}", c.merchantId(), ex.getMessage());
                }
            }
            if (created > 0) {
                log.info("Auto-payout: created {} payout request(s)", created);
            }
        } catch (Exception ex) {
            log.error("Auto-payout sweep failed", ex);
        }
    }
}
