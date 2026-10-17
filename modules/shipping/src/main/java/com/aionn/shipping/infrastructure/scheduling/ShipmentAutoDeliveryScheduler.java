package com.aionn.shipping.infrastructure.scheduling;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import com.aionn.shipping.infrastructure.config.ShippingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "shipping.auto-delivery", name = "enabled", havingValue = "true")
public class ShipmentAutoDeliveryScheduler {

    private final ShipmentPersistencePort shipmentRepository;
    private final ShipmentStatusApplier shipmentStatusApplier;
    private final ShippingProperties properties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${shipping.auto-delivery.delay-ms:1200000}")
    @SchedulerLock(name = "shipping-auto-delivery", lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    public void processAutoDelivery() {
        try {
            ShippingProperties.AutoDelivery config = properties.autoDelivery();
            long thresholdSec = config.transitThresholdSeconds();
            int batchSize = config.batchSize();

            Instant cutoff = clock.instant().minusSeconds(thresholdSec);
            List<Shipment> inTransitShipments = shipmentRepository.findInTransitOlderThan(cutoff, batchSize);

            if (inTransitShipments.isEmpty()) {
                return;
            }

            log.info("Found {} shipment(s) in transit past {}s threshold for auto-delivery",
                    inTransitShipments.size(), thresholdSec);

            for (Shipment shipment : inTransitShipments) {
                try {
                    CarrierClient.OrderDetail mockDetail = new CarrierClient.OrderDetail(
                            "delivered",
                            "Auto-Delivery Fulfillment Hub",
                            "Auto-Delivery Carrier Service",
                            "0900000000",
                            "https://static.aionn.com/signatures/auto_delivered.png",
                            null,
                            "WH_AUTO",
                            clock.instant()
                    );

                    shipmentStatusApplier.apply(shipment.getShipmentId(), ShipmentStatus.DELIVERED, mockDetail);
                    log.info("Auto-delivered shipment {} (order: {}, tracking: {})",
                            shipment.getShipmentId(), shipment.getOrderId(), shipment.getTrackingCode());
                } catch (Exception ex) {
                    log.error("Failed to auto-deliver shipment {}: {}", shipment.getShipmentId(), ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.error("Auto delivery scheduler run failed", ex);
        }
    }
}

