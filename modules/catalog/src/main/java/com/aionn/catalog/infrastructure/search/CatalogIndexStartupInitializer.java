package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogIndexStartupInitializer {

    private final ProductService productService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {
        log.info("Application ready; triggering catalog search index sync...");
        try {
            productService.syncAllToSearchIndex();
        } catch (RuntimeException ex) {
            log.error("Failed to sync catalog products to search index on startup", ex);
        }
    }
}
