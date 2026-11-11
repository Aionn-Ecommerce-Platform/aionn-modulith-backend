package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.mapper.ProductSearchDocumentMapper;
import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.catalog.domain.model.AttributeTemplate.AttributeDefinition;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Performs the actual search-index writes away from the caller.
 *
 * <p>OpenSearch calls are HTTP round trips: running them inside a domain-event listener puts them on the
 * outbox dispatcher thread, where one slow or unreachable cluster blocks order, payment and shipping
 * events for every module. The listeners therefore hand the work to {@code searchIndexTaskExecutor}.
 *
 * <p>That executor is single-threaded on purpose. Index operations for the same product must apply in
 * the order they were produced, otherwise a stale document can overwrite a newer one. A single worker
 * also keeps bulk merchant reindexing from competing with itself.
 *
 * <p>Search documents are derived data and the catalog is reindexed from the database on startup, so a
 * task lost to a crash is recoverable rather than a durability bug.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CatalogSearchIndexWriter {

    private static final String SEARCH_INDEX_EXECUTOR = "searchIndexTaskExecutor";
    private static final int PAGE_SIZE = 100;

    private final ProductPersistencePort productRepository;
    private final ProductSearchIndex searchIndex;
    private final ProductSearchDocumentMapper searchDocumentMapper;
    private final AttributeTemplatePersistencePort attributeTemplateRepository;
    private final ProductReviewPersistencePort reviewRepository;
    private final ProductSoldCounterPersistencePort soldCounterRepository;
    private final TransactionTemplate transactionTemplate;

    /** Indexes a product regardless of status, used after publish or restore. */
    @Async(SEARCH_INDEX_EXECUTOR)
    public void reindexAsync(String productId) {
        reindex(productId, false);
    }

    /** Indexes a product only when it is searchable, used after variant/metadata changes. */
    @Async(SEARCH_INDEX_EXECUTOR)
    public void reindexIfSearchableAsync(String productId) {
        reindex(productId, true);
    }

    @Async(SEARCH_INDEX_EXECUTOR)
    public void removeAsync(String productId) {
        searchIndex.remove(productId);
    }

    @Async(SEARCH_INDEX_EXECUTOR)
    public void reindexMerchantAsync(String merchantId) {
        log.info("Reindexing products of merchant {}", merchantId);
        forEachPage(merchantId, products -> {
            List<ProductSearchDocument> docs = products.stream()
                    .filter(product -> product.getStatus() == ProductStatus.PUBLISHED)
                    .map(this::buildSearchDocument)
                    .toList();
            searchIndex.indexAll(docs);
        });
    }

    @Async(SEARCH_INDEX_EXECUTOR)
    public void removeMerchantAsync(String merchantId) {
        log.info("Removing products of merchant {}", merchantId);
        forEachPage(merchantId,
                products -> searchIndex.removeAll(products.stream().map(Product::getProductId).toList()));
    }

    private void reindex(String productId, boolean searchableOnly) {
        ProductSearchDocument document = transactionTemplate.execute(status -> productRepository
                .findById(productId)
                .filter(product -> !searchableOnly || product.getStatus().isSearchable())
                .map(this::buildSearchDocument)
                .orElse(null));
        if (document != null) {
            searchIndex.index(document);
        }
    }

    private void forEachPage(String merchantId, Consumer<List<Product>> handler) {
        int page = 0;
        while (true) {
            List<Product> batch = productRepository.listByMerchant(
                    merchantId, OffsetPagination.of(page, PAGE_SIZE));
            if (batch.isEmpty()) {
                return;
            }
            handler.accept(batch);
            if (batch.size() < PAGE_SIZE) {
                return;
            }
            page++;
        }
    }

    private ProductSearchDocument buildSearchDocument(Product product) {
        double rating = reviewRepository.getAverageRating(product.getProductId());
        long soldCount = soldCounterRepository.getSoldCount(product.getProductId());
        return searchDocumentMapper.toSearchDocument(
                product, collectFilterableAttributes(product), rating, soldCount);
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

    private static void addFilterable(AttributeTemplate template, Product product,
            Map<String, String> filterable) {
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
