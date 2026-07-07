package com.aionn.catalog.infrastructure.listener;

import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.mapper.ProductSearchDocumentMapper;
import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import com.aionn.catalog.domain.event.MerchantEvents;
import com.aionn.catalog.domain.model.AttributeTemplate.AttributeDefinition;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Reacts to merchant lifecycle events by hiding or reindexing that merchant's
// products in the faceted search index, keeping the storefront consistent.
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantSearchSyncListener {

    private static final int PAGE_SIZE = 100;

    private final ProductPersistencePort productRepository;
    private final ProductSearchIndex searchIndex;
    private final ProductSearchDocumentMapper searchDocumentMapper;
    private final AttributeTemplatePersistencePort attributeTemplateRepository;

    @EventListener
    public void onSuspended(MerchantEvents.MerchantSuspended event) {
        log.info("Hiding products of suspended merchant {}", event.merchantId());
        removeAll(event.merchantId());
    }

    @EventListener
    public void onClosed(MerchantEvents.MerchantClosed event) {
        log.info("Removing products of closed merchant {}", event.merchantId());
        removeAll(event.merchantId());
    }

    @EventListener
    public void onActivated(MerchantEvents.MerchantActivated event) {
        log.info("Reindexing products of activated merchant {}", event.merchantId());
        reindexAllPublished(event.merchantId());
    }

    @EventListener
    public void onProfileUpdated(MerchantEvents.MerchantProfileUpdated event) {
        if (!event.provinceChanged()) {
            return;
        }
        log.info("Reindexing products of merchant {} after province change", event.merchantId());
        reindexAllPublished(event.merchantId());
    }

    private void removeAll(String merchantId) {
        forEachPage(merchantId,
                products -> searchIndex.removeAll(products.stream().map(Product::getProductId).toList()));
    }

    private void reindexAllPublished(String merchantId) {
        forEachPage(merchantId, products -> {
            List<ProductSearchDocument> docs = products.stream()
                    .filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                    .map(this::buildSearchDocument)
                    .toList();
            searchIndex.indexAll(docs);
        });
    }

    private void forEachPage(String merchantId, Consumer<List<Product>> handler) {
        int page = 0;
        while (true) {
            List<Product> batch = productRepository.listByMerchant(merchantId, OffsetPagination.of(page, PAGE_SIZE));
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
        Map<String, String> filterable = new LinkedHashMap<>();
        if (!product.attributes().isEmpty()) {
            for (String categoryId : product.categoryIds()) {
                attributeTemplateRepository.findByCategoryId(categoryId).ifPresent(template -> {
                    for (Map.Entry<String, AttributeDefinition> def : template.snapshot().entrySet()) {
                        if (def.getValue().filterable()) {
                            String value = product.attributes().get(def.getKey());
                            if (value != null) {
                                filterable.put(def.getKey(), value);
                            }
                        }
                    }
                });
            }
        }
        return searchDocumentMapper.toSearchDocument(product, filterable);
    }
}
