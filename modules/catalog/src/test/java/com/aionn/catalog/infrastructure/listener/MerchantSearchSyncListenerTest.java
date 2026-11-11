package com.aionn.catalog.infrastructure.listener;

import com.aionn.catalog.domain.event.MerchantEvents;
import com.aionn.catalog.infrastructure.search.CatalogSearchIndexWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Merchant lifecycle changes must be queued for the async search writer rather than reindexing inline:
 * a single activation can touch hundreds of products, and this listener runs on the outbox thread.
 */
@ExtendWith(MockitoExtension.class)
class MerchantSearchSyncListenerTest {

    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String ADMIN_ID = "01HZADM0000000000000000001";

    @Mock
    private CatalogSearchIndexWriter writer;

    @InjectMocks
    private MerchantSearchSyncListener listener;

    @Test
    void suspensionQueuesMerchantRemoval() {
        listener.onSuspended(new MerchantEvents.MerchantSuspended(
                MERCHANT_ID, "fraud", ADMIN_ID, Instant.now(), Instant.now()));

        verify(writer).removeMerchantAsync(MERCHANT_ID);
    }

    @Test
    void closureQueuesMerchantRemoval() {
        listener.onClosed(new MerchantEvents.MerchantClosed(
                MERCHANT_ID, "shutdown", Instant.now(), Instant.now()));

        verify(writer).removeMerchantAsync(MERCHANT_ID);
    }

    @Test
    void activationQueuesMerchantReindex() {
        listener.onActivated(new MerchantEvents.MerchantActivated(
                MERCHANT_ID, ADMIN_ID, "ok", Instant.now(), Instant.now()));

        verify(writer).reindexMerchantAsync(MERCHANT_ID);
    }

    @Test
    void provinceChangeQueuesMerchantReindex() {
        listener.onProfileUpdated(new MerchantEvents.MerchantProfileUpdated(
                MERCHANT_ID, "Name", null, null, "79", "HCM", true, Instant.now()));

        verify(writer).reindexMerchantAsync(MERCHANT_ID);
    }

    @Test
    void unchangedProvinceDoesNothing() {
        listener.onProfileUpdated(new MerchantEvents.MerchantProfileUpdated(
                MERCHANT_ID, "Name", null, null, "01", "Ha Noi", false, Instant.now()));

        verify(writer, never()).reindexMerchantAsync(MERCHANT_ID);
        verify(writer, never()).removeMerchantAsync(MERCHANT_ID);
    }
}
