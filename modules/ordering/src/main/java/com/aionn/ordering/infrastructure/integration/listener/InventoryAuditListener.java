package com.aionn.ordering.infrastructure.integration.listener;

import com.aionn.sharedkernel.integration.event.inventory.StockCommittedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReservationFailedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class InventoryAuditListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(StockCommittedIntegrationEvent event) {
        log.info("[ORDER-AUDIT] StockCommitted order={} reservation={} sku={} qty={}",
                event.orderId(), event.reservationId(), event.skuId(), event.quantity());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(StockReservationFailedIntegrationEvent event) {
        log.info("[ORDER-AUDIT] StockReservationFailed order={} sku={} warehouse={} qty={} reason={}",
                event.orderId(), event.skuId(), event.warehouseId(), event.quantity(), event.reason());
    }
}
