package com.aionn.ordering.infrastructure.integration.listener;

import com.aionn.sharedkernel.integration.event.inventory.StockCommittedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReservationFailedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryAuditListener {

    @EventListener
    public void on(StockCommittedIntegrationEvent event) {
        log.info("[ORDER-AUDIT] StockCommitted order={} reservation={} sku={} qty={}",
                event.orderId(), event.reservationId(), event.skuId(), event.quantity());
    }

    @EventListener
    public void on(StockReservationFailedIntegrationEvent event) {
        log.info("[ORDER-AUDIT] StockReservationFailed order={} sku={} warehouse={} qty={} reason={}",
                event.orderId(), event.skuId(), event.warehouseId(), event.quantity(), event.reason());
    }
}
