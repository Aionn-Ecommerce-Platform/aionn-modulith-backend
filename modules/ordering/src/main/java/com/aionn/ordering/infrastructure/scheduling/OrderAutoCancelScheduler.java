package com.aionn.ordering.infrastructure.scheduling;

import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.infrastructure.config.OrderingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ordering.auto-cancel", name = "enabled", havingValue = "true")
public class OrderAutoCancelScheduler {

    private final OrderPersistencePort orderRepository;
    private final OrderAutoCancelWorker worker;
    private final OrderingProperties properties;

    @Scheduled(fixedDelayString = "${ordering.auto-cancel.delay-ms:60000}")
    public void run() {
        try {
            int timeoutMinutes = properties.autoCancel().timeoutMinutes();
            int batchSize = properties.autoCancel().batchSize();
            Instant cutoff = Instant.now().minus(Duration.ofMinutes(timeoutMinutes));
            List<String> pendingIds = orderRepository.findPendingOrderIdsOlderThan(cutoff, batchSize);
            int cancelled = 0;
            for (String orderId : pendingIds) {
                try {
                    worker.cancelOneExpired(orderId);
                    cancelled++;
                } catch (org.springframework.dao.OptimisticLockingFailureException ex) {
                    log.debug("Skip auto-cancel for {}: concurrent state change", orderId);
                } catch (RuntimeException ex) {
                    log.warn("Skip auto-cancel for {}: {}", orderId, ex.getMessage());
                }
            }
            if (cancelled > 0) {
                log.info("Auto-cancelled {} order(s) older than {} minutes", cancelled, timeoutMinutes);
            }
        } catch (Exception ex) {
            log.error("Auto-cancel sweep failed", ex);
        }
    }
}
