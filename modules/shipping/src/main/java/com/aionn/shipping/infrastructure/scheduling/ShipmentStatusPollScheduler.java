package com.aionn.shipping.infrastructure.scheduling;

import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.infrastructure.config.ShippingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "shipping.status-poller", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ShipmentStatusPollScheduler {

    private final ShipmentPersistencePort shipmentRepository;
    private final ShipmentStatusPollWorker worker;
    private final ShippingProperties properties;

    @Scheduled(fixedDelayString = "${shipping.status-poller.delay-ms:60000}")
    public void pollActive() {
        try {
            int batchSize = properties.statusPoller().batchSize();
            List<Shipment> active = shipmentRepository.findActiveTracking(batchSize);
            if (active.isEmpty()) {
                return;
            }
            for (Shipment shipment : active) {
                worker.syncOne(shipment.getShipmentId());
            }
            log.debug("Polled {} active shipment(s) from carrier", active.size());
        } catch (Exception ex) {
            log.error("Shipment status poll failed", ex);
        }
    }
}
