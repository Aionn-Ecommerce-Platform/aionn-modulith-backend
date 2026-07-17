package com.aionn.inventory.infrastructure.listener;

import com.aionn.inventory.application.port.out.WarehousePersistencePort;
import com.aionn.inventory.domain.model.Warehouse;
import com.aionn.inventory.domain.valueobject.WarehouseStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.event.catalog.MerchantActivatedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.MerchantClosedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.MerchantSuspendedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component("inventoryMerchantLifecycleListener")
@RequiredArgsConstructor
public class MerchantLifecycleListener {

    private final WarehousePersistencePort warehouseRepository;
    private final EventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSuspended(MerchantSuspendedIntegrationEvent event) {
        log.info("Merchant suspended, updating warehouses: merchantId={}", event.merchantId());
        applyToAllWarehouses(event.merchantId(), WarehouseStatus.SUSPENDED, "system",
                "merchant-suspended:" + event.reason());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClosed(MerchantClosedIntegrationEvent event) {
        log.info("Merchant closed, suspending warehouses: merchantId={}", event.merchantId());
        applyToAllWarehouses(event.merchantId(), WarehouseStatus.SUSPENDED, "system",
                "merchant-closed:" + event.reason());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onActivated(MerchantActivatedIntegrationEvent event) {
        log.info("Merchant activated, lifting warehouse suspensions: merchantId={}", event.merchantId());
        List<Warehouse> warehouses = warehouseRepository.findByMerchantOrderByPriority(event.merchantId());
        for (Warehouse warehouse : warehouses) {
            if (warehouse.getStatus() == WarehouseStatus.SUSPENDED) {
                warehouse.liftSuspension();
                warehouseRepository.save(warehouse);
                eventPublisher.publish(warehouse.pullEvents());
            }
        }
    }

    private void applyToAllWarehouses(String merchantId, WarehouseStatus targetStatus,
            String adminId, String reason) {
        List<Warehouse> warehouses = warehouseRepository.findByMerchantOrderByPriority(merchantId);
        for (Warehouse warehouse : warehouses) {
            if (warehouse.getStatus() == targetStatus) {
                continue;
            }
            try {
                warehouse.suspend(adminId, reason);
                warehouseRepository.save(warehouse);
                eventPublisher.publish(warehouse.pullEvents());
            } catch (Exception ex) {
                log.warn("Cannot suspend warehouse {} for merchant {}: {}",
                        warehouse.getWarehouseId(), merchantId, ex.getMessage());
            }
        }
    }
}
