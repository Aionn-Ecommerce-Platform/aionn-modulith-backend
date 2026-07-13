package com.aionn.inventory.infrastructure.integration;

import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.event.InventoryItemEvents;
import com.aionn.inventory.domain.event.StockReservationEvents;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryIntegrationEventPublisher {

    private final IntegrationEventPublisher integrationEventPublisher;
    private final InventoryIntegrationEventMapper mapper;
    private final WarehousePersistencePort warehouseRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStockReserved(StockReservationEvents.StockReserved event) {
        log.debug("Publishing StockReservedIntegrationEvent for reservation: {}", event.reservationId());
        integrationEventPublisher.publish(mapper.toIntegrationEvent(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStockReservationFailed(StockReservationEvents.StockReservationFailed event) {
        log.debug("Publishing StockReservationFailedIntegrationEvent for reservation: {}", event.reservationId());
        integrationEventPublisher.publish(mapper.toIntegrationEvent(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStockCommitted(StockReservationEvents.StockCommitted event) {
        log.debug("Publishing StockCommittedIntegrationEvent for reservation: {}", event.reservationId());
        integrationEventPublisher.publish(mapper.toIntegrationEvent(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStockReleased(StockReservationEvents.StockReleased event) {
        log.debug("Publishing StockReleasedIntegrationEvent for reservation: {}", event.reservationId());
        integrationEventPublisher.publish(mapper.toIntegrationEvent(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onSafetyStockBreached(InventoryItemEvents.SafetyStockBreached event) {
        String merchantId = warehouseRepository.findById(event.warehouseId())
                .map(w -> w.getMerchantId())
                .orElse(null);
        if (merchantId == null) {
            log.warn("Cannot publish SafetyStockBreachedIntegrationEvent: warehouse {} not found",
                    event.warehouseId());
            return;
        }
        log.debug("Publishing SafetyStockBreachedIntegrationEvent for sku={} warehouse={}",
                event.skuId(), event.warehouseId());
        integrationEventPublisher.publish(mapper.toIntegrationEvent(merchantId, event));
    }
}
