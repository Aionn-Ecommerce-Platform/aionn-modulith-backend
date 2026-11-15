package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
public class CatalogIndexStartupInitializer {

    private final ProductService productService;
    private final ObjectProvider<OpenSearchIndexInitializer> indexInitializer;

    public CatalogIndexStartupInitializer(
            ProductService productService,
            ObjectProvider<OpenSearchIndexInitializer> indexInitializer) {
        this.productService = productService;
        this.indexInitializer = indexInitializer;
    }

    @Order(1)
    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {
        log.info("Application ready; triggering catalog search index sync...");
        indexInitializer.ifAvailable(OpenSearchIndexInitializer::ensureIndexExists);
        try {
            productService.syncAllToSearchIndex();
        } catch (RuntimeException ex) {
            log.error("Failed to sync catalog products to search index on startup", ex);
        }
    }
}
