package com.aionn.ordering.infrastructure.scheduling;

import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.application.service.OrderService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ordering.shipment-reconcile", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ShipmentRegistrationReconciliationScheduler {

    private static final int BATCH_SIZE = 100;
    private static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private final OrderPersistencePort orderPersistencePort;
    private final ShippingGateway shippingGateway;
    private final OrderService orderService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${ordering.shipment-reconcile.delay-ms:60000}")
    @SchedulerLock(name = "ordering-shipment-reconcile", lockAtMostFor = "PT5M", lockAtLeastFor = "PT25S")
    public void run() {
        try {
            Instant cutoff = clock.instant().minus(STALE_AFTER);
            List<String> orderIds = orderPersistencePort.findPreparingOrderIdsUpdatedBefore(cutoff, BATCH_SIZE);
            for (String orderId : orderIds) {
                try {
                    if (!shippingGateway.hasActiveShipment(orderId)
                            || shippingGateway.hasUnregisteredShipment(orderId)) {
                        orderService.enqueueShipmentRegistration(orderId);
                    }
                } catch (RuntimeException ex) {
                    log.warn("Shipment reconciliation failed for order {}: {}", orderId, ex.getMessage());
                }
            }
        } catch (RuntimeException ex) {
            log.error("Shipment registration reconciliation sweep failed", ex);
        }
    }
}
