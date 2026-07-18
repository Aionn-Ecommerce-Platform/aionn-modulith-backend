package com.aionn.ordering.infrastructure.listener;

import com.aionn.sharedkernel.integration.event.catalog.ProductEmergencyTakedownIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogLifecycleListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductTakedown(ProductEmergencyTakedownIntegrationEvent event) {
        log.info("Catalog emergency takedown: product={} admin={} - downstream cancellation handled per-SKU",
                event.productId(), event.adminId());
    }
}
