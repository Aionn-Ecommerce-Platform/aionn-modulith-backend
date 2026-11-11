package com.aionn.catalog.infrastructure.listener;

import com.aionn.catalog.application.service.StockAlertService;
import com.aionn.contracts.event.inventory.StockReplenishedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockAlertEventListener {

    private final StockAlertService stockAlertService;

    @EventListener
    public void on(StockReplenishedIntegrationEvent event) {
        stockAlertService.notifyRestocked(event.skuId(), event.occurredAt());
    }
}
