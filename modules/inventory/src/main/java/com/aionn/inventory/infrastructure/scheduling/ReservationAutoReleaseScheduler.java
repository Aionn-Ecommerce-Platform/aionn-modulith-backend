package com.aionn.inventory.infrastructure.scheduling;

import com.aionn.inventory.application.port.out.StockReservationPersistencePort;
import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.infrastructure.config.InventoryProperties;
import com.aionn.contracts.port.ordering.OrderQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.time.Clock;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "inventory.reservation.auto-release", name = "enabled", havingValue = "true")
public class ReservationAutoReleaseScheduler {

    private static final Set<String> ACTIVE_ORDER_STATUSES = Set.of("APPROVED", "PREPARING", "SHIPPED");

    private final StockReservationPersistencePort stockReservationPersistencePort;
    private final ReservationAutoReleaseWorker worker;
    private final InventoryProperties properties;
    private final OrderQueryPort orderQueryPort;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${inventory.reservation.auto-release.delay-ms:30000}")
    @SchedulerLock(name = "inventory-reservation-auto-release", lockAtMostFor = "PT10M", lockAtLeastFor = "PT25S")
    public void releaseExpired() {
        try {
            int batchSize = properties.reservation().autoRelease().batchSize();
            List<StockReservation> expired = stockReservationPersistencePort.findExpired(clock.instant(), batchSize);
            int released = 0;
            for (StockReservation reservation : expired) {
                try {
                    if (orderStillActive(reservation.getOrderId())) {
                        log.debug("Keeping reservation {} alive for active order {}",
                                reservation.getReservationId(), reservation.getOrderId());
                        continue;
                    }
                    worker.releaseOne(reservation.getReservationId());
                    released++;
                } catch (RuntimeException ex) {
                    log.warn("Skip expired reservation {}: {}", reservation.getReservationId(), ex.getMessage());
                }
            }
            if (released > 0) {
                log.info("Auto-released {} expired reservation(s)", released);
            }
        } catch (Exception ex) {
            log.error("Failed to auto-release expired reservations", ex);
        }
    }

    private boolean orderStillActive(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return false;
        }
        return orderQueryPort.findOrderSummary(orderId)
                .map(order -> order.status() != null && ACTIVE_ORDER_STATUSES.contains(order.status()))
                .orElse(false);
    }
}
