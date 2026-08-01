package com.aionn.ordering.infrastructure.listener;

import com.aionn.sharedkernel.integration.event.catalog.ProductEmergencyTakedownIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogLifecycleListener {

    @EventListener
    public void onProductTakedown(ProductEmergencyTakedownIntegrationEvent event) {
        log.info("Catalog emergency takedown: product={} admin={} - downstream cancellation handled per-SKU",
                event.productId(), event.adminId());
    }
}
