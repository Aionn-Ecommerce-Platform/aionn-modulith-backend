package com.aionn.catalog.infrastructure.listener;

import com.aionn.catalog.domain.event.MerchantEvents;
import com.aionn.catalog.infrastructure.search.CatalogSearchIndexWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps the catalog index consistent with merchant lifecycle changes.
 *
 * <p>A bulk merchant reindex can touch hundreds of products, so it is queued onto the dedicated
 * single-threaded search executor instead of running on the outbox dispatcher thread.
 */
@Component
@RequiredArgsConstructor
public class MerchantSearchSyncListener {

    private final CatalogSearchIndexWriter writer;

    @EventListener
    public void onSuspended(MerchantEvents.MerchantSuspended event) {
        writer.removeMerchantAsync(event.merchantId());
    }

    @EventListener
    public void onClosed(MerchantEvents.MerchantClosed event) {
        writer.removeMerchantAsync(event.merchantId());
    }

    @EventListener
    public void onActivated(MerchantEvents.MerchantActivated event) {
        writer.reindexMerchantAsync(event.merchantId());
    }

    @EventListener
    public void onProfileUpdated(MerchantEvents.MerchantProfileUpdated event) {
        if (!event.provinceChanged()) {
            return;
        }
        writer.reindexMerchantAsync(event.merchantId());
    }
}
