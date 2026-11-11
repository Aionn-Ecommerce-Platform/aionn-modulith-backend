package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.domain.event.ProductEvents;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The listeners are adapters now: they must hand work to the async writer and never touch OpenSearch
 * directly, because they execute on the outbox dispatcher thread.
 */
@ExtendWith(MockitoExtension.class)
class ProductSearchIndexUpdaterTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String ADMIN_ID = "01HZADM0000000000000000001";

    @Mock
    private CatalogSearchIndexWriter writer;

    @InjectMocks
    private ProductSearchIndexUpdater updater;

    @Test
    void publishingAndRestoringQueueAFullReindex() {
        updater.onProductPublished(new ProductEvents.ProductPublished(
                PRODUCT_ID, ADMIN_ID, Instant.now(), Instant.now()));
        updater.onProductRestored(new ProductEvents.ProductRestored(
                PRODUCT_ID, Instant.now(), Instant.now()));

        verify(writer, times(2)).reindexAsync(PRODUCT_ID);
    }

    @Test
    void hidingEventsQueueARemoval() {
        updater.onProductRejected(new ProductEvents.ProductRejected(
                PRODUCT_ID, ADMIN_ID, "SPAM", "bad", Instant.now()));
        updater.onProductDeactivated(new ProductEvents.ProductDeactivated(
                PRODUCT_ID, MERCHANT_ID, "policy", Instant.now(), Instant.now()));
        updater.onProductEmergencyTakedown(new ProductEvents.ProductEmergencyTakedown(
                PRODUCT_ID, ADMIN_ID, "abuse", Instant.now(), Instant.now()));

        verify(writer, times(3)).removeAsync(PRODUCT_ID);
    }

    @Test
    void searchableMutationsQueueAConditionalReindex() {
        updater.onProductVariantDefined(new ProductEvents.ProductVariantDefined(
                PRODUCT_ID, "sku-1", Map.of(), Instant.now()));
        updater.onProductVariantRemoved(new ProductEvents.ProductVariantRemoved(
                PRODUCT_ID, "sku-1", MERCHANT_ID, Instant.now(), Instant.now()));
        updater.onProductVariantPriceChanged(new ProductEvents.ProductVariantPriceChanged(
                PRODUCT_ID, "sku-1", null, new BigDecimal("10"), "VND", Instant.now()));
        updater.onProductMediaUpdated(new ProductEvents.ProductMediaUpdated(
                PRODUCT_ID, List.of("img"), Instant.now()));
        updater.onProductBrandAssigned(new ProductEvents.ProductBrandAssigned(
                PRODUCT_ID, "b1", Instant.now()));
        updater.onProductCategorized(new ProductEvents.ProductCategorized(
                PRODUCT_ID, List.of("c1"), Instant.now()));
        updater.onProductMetadataUpdated(new ProductEvents.ProductMetadataUpdated(
                PRODUCT_ID, List.of("tag"), "desc", Instant.now()));
        updater.onProductCollectionAssigned(new ProductEvents.ProductCollectionAssigned(
                PRODUCT_ID, List.of("col"), Instant.now()));
        updater.onProductAttributesDefined(new ProductEvents.ProductAttributesDefined(
                PRODUCT_ID, Map.of("color", "red"), Instant.now()));

        verify(writer, times(9)).reindexIfSearchableAsync(PRODUCT_ID);
    }

    @Test
    void soldCounterReindexUsesTheSearchablePath() {
        updater.reindexProduct(PRODUCT_ID);

        verify(writer).reindexIfSearchableAsync(PRODUCT_ID);
        verify(writer, never()).reindexAsync(PRODUCT_ID);
    }
}
