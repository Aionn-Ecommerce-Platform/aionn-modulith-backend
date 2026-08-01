package com.aionn.notification.infrastructure.listener;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.sharedkernel.integration.event.inventory.SafetyStockBreachedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockCommittedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.inventory.StockReservationFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private static final String EVENT_SAFETY_STOCK_BREACHED = "inventory.safety-stock-breached";

    private final NotificationDeliveryOrchestrator deliveryOrchestrator;
    private final MerchantQueryPort merchantQueryPort;

    @EventListener
    public void on(SafetyStockBreachedIntegrationEvent event) {
        String ownerId = merchantQueryPort.findOwnerIdByMerchantId(event.merchantId()).orElse(null);
        if (ownerId == null) {
            log.warn("SafetyStockBreached: cannot resolve owner for merchant {}; skipping notification",
                    event.merchantId());
            return;
        }
        Map<String, String> context = new HashMap<>();
        context.put("eventId", event.eventId());
        context.put("occurredAt", event.occurredAt().toString());
        context.put("merchantId", event.merchantId());
        context.put("skuId", event.skuId());
        context.put("warehouseId", event.warehouseId());
        context.put("availableQty", String.valueOf(event.availableQty()));
        context.put("safetyStockQty", String.valueOf(event.safetyStockQty()));
        deliveryOrchestrator.sendByEvent(new NotificationCommands.SendByEvent(
                ownerId, EVENT_SAFETY_STOCK_BREACHED, NotificationCategory.SYSTEM,
                null, null, null, context));
    }

    /**
     * Stock commit / reservation failure are not user-facing alerts today.
     * Audit-log only so the cross-module flow is observable; downstream
     * recipients can be added once warehouse → owner resolution is wired.
     */
    @EventListener
    public void on(StockCommittedIntegrationEvent event) {
        log.info("[INTEGRATION] StockCommitted reservationId={} sku={} warehouse={} order={} qty={}",
                event.reservationId(), event.skuId(), event.warehouseId(), event.orderId(), event.quantity());
    }

    @EventListener
    public void on(StockReservationFailedIntegrationEvent event) {
        log.info("[INTEGRATION] StockReservationFailed sku={} warehouse={} order={} qty={} reason={}",
                event.skuId(), event.warehouseId(), event.orderId(), event.quantity(), event.reason());
    }
}
