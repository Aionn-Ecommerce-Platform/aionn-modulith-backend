package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.mapper.ProductSearchDocumentMapper;
import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import com.aionn.catalog.domain.event.ProductEvents;
import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.catalog.domain.model.AttributeTemplate.AttributeDefinition;
import com.aionn.catalog.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

// Keeps the faceted search index in sync after Product domain events commit.
// Runs after commit so slow search latency never extends business transactions.
@Component
@RequiredArgsConstructor
public class ProductSearchIndexUpdater {

    private final ProductPersistencePort productRepository;
    private final ProductSearchIndex searchIndex;
    private final ProductSearchDocumentMapper searchDocumentMapper;
    private final AttributeTemplatePersistencePort attributeTemplateRepository;
    private final ProductReviewPersistencePort reviewRepository;
    private final ProductSoldCounterPersistencePort soldCounterRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductPublished(ProductEvents.ProductPublished event) {
        reindex(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductRestored(ProductEvents.ProductRestored event) {
        reindex(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductRejected(ProductEvents.ProductRejected event) {
        searchIndex.remove(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductDeactivated(ProductEvents.ProductDeactivated event) {
        searchIndex.remove(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductEmergencyTakedown(ProductEvents.ProductEmergencyTakedown event) {
        searchIndex.remove(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductVariantDefined(ProductEvents.ProductVariantDefined event) {
        reindexIfSearchable(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductVariantRemoved(ProductEvents.ProductVariantRemoved event) {
        reindexIfSearchable(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductVariantPriceChanged(ProductEvents.ProductVariantPriceChanged event) {
        reindexIfSearchable(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductMediaUpdated(ProductEvents.ProductMediaUpdated event) {
        reindexIfSearchable(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductBrandAssigned(ProductEvents.ProductBrandAssigned event) {
        reindexIfSearchable(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductCategorized(ProductEvents.ProductCategorized event) {
        reindexIfSearchable(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductMetadataUpdated(ProductEvents.ProductMetadataUpdated event) {
        reindexIfSearchable(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductCollectionAssigned(ProductEvents.ProductCollectionAssigned event) {
        reindexIfSearchable(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onProductAttributesDefined(ProductEvents.ProductAttributesDefined event) {
        reindexIfSearchable(event.productId());
    }

    private void reindex(String productId) {
        productRepository.findById(productId)
                .ifPresent(product -> searchIndex.index(buildSearchDocument(product)));
    }

    private void reindexIfSearchable(String productId) {
        productRepository.findById(productId).ifPresent(product -> {
            if (product.getStatus().isSearchable()) {
                searchIndex.index(buildSearchDocument(product));
            }
        });
    }

    private ProductSearchDocument buildSearchDocument(Product product) {
        double rating = reviewRepository.getAverageRating(product.getProductId());
        long soldCount = soldCounterRepository.getSoldCount(product.getProductId());
        return searchDocumentMapper.toSearchDocument(product, collectFilterableAttributes(product), rating, soldCount);
    }

    private Map<String, String> collectFilterableAttributes(Product product) {
        Map<String, String> filterable = new LinkedHashMap<>();
        if (product.attributes().isEmpty()) {
            return filterable;
        }
        for (String categoryId : product.categoryIds()) {
            attributeTemplateRepository.findByCategoryId(categoryId)
                    .ifPresent(template -> addFilterable(template, product, filterable));
        }
        return filterable;
    }

    private static void addFilterable(AttributeTemplate template, Product product, Map<String, String> filterable) {
        for (Map.Entry<String, AttributeDefinition> def : template.snapshot().entrySet()) {
            if (!def.getValue().filterable()) {
                continue;
            }
            String value = product.attributes().get(def.getKey());
            if (value != null) {
                filterable.put(def.getKey(), value);
            }
        }
    }
}
