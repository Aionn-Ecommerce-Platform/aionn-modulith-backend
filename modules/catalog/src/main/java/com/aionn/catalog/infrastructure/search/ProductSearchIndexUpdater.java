package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.domain.event.ProductEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates product domain events into search-index work.
 *
 * <p>Nothing here touches OpenSearch: the {@link CatalogSearchIndexWriter} hands the write to the
 * single-threaded {@code searchIndexTaskExecutor}. Listeners run on the outbox dispatcher thread, and a
 * product publish should cost that thread one queue submission, not an HTTP round trip.
 */
@Component
@RequiredArgsConstructor
public class ProductSearchIndexUpdater {

    private final CatalogSearchIndexWriter writer;

    @EventListener
    public void onProductPublished(ProductEvents.ProductPublished event) {
        writer.reindexAsync(event.productId());
    }

    @EventListener
    public void onProductRestored(ProductEvents.ProductRestored event) {
        writer.reindexAsync(event.productId());
    }

    @EventListener
    public void onProductRejected(ProductEvents.ProductRejected event) {
        writer.removeAsync(event.productId());
    }

    @EventListener
    public void onProductDeactivated(ProductEvents.ProductDeactivated event) {
        writer.removeAsync(event.productId());
    }

    @EventListener
    public void onProductEmergencyTakedown(ProductEvents.ProductEmergencyTakedown event) {
        writer.removeAsync(event.productId());
    }

    @EventListener
    public void onProductVariantDefined(ProductEvents.ProductVariantDefined event) {
        writer.reindexIfSearchableAsync(event.productId());
    }

    @EventListener
    public void onProductVariantRemoved(ProductEvents.ProductVariantRemoved event) {
        writer.reindexIfSearchableAsync(event.productId());
    }

    @EventListener
    public void onProductVariantPriceChanged(ProductEvents.ProductVariantPriceChanged event) {
        writer.reindexIfSearchableAsync(event.productId());
    }

    @EventListener
    public void onProductMediaUpdated(ProductEvents.ProductMediaUpdated event) {
        writer.reindexIfSearchableAsync(event.productId());
    }

    @EventListener
    public void onProductBrandAssigned(ProductEvents.ProductBrandAssigned event) {
        writer.reindexIfSearchableAsync(event.productId());
    }

    @EventListener
    public void onProductCategorized(ProductEvents.ProductCategorized event) {
        writer.reindexIfSearchableAsync(event.productId());
    }

    @EventListener
    public void onProductMetadataUpdated(ProductEvents.ProductMetadataUpdated event) {
        writer.reindexIfSearchableAsync(event.productId());
    }

    @EventListener
    public void onProductCollectionAssigned(ProductEvents.ProductCollectionAssigned event) {
        writer.reindexIfSearchableAsync(event.productId());
    }

    @EventListener
    public void onProductAttributesDefined(ProductEvents.ProductAttributesDefined event) {
        writer.reindexIfSearchableAsync(event.productId());
    }

    /** Reindex after a non-domain change such as a sold-counter increment. */
    public void reindexProduct(String productId) {
        writer.reindexIfSearchableAsync(productId);
    }
}
