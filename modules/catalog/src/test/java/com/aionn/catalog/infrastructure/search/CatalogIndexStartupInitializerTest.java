package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CatalogIndexStartupInitializerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private CatalogIndexStartupInitializer initializer;

    @Test
    void triggersSyncOnStartup() {
        initializer.initializeIndex();
        verify(productService).syncAllToSearchIndex();
    }

    @Test
    void swallowsSyncFailure() {
        doThrow(new RuntimeException("boom")).when(productService).syncAllToSearchIndex();

        initializer.initializeIndex();

        verify(productService).syncAllToSearchIndex();
    }
}
