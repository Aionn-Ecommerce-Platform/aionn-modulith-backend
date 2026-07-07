package com.aionn.catalog.infrastructure.listener;

import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.mapper.ProductSearchDocumentMapper;
import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import com.aionn.catalog.domain.event.MerchantEvents;
import com.aionn.catalog.domain.model.Product;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantSearchSyncListenerTest {

    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String ADMIN_ID = "01HZADM0000000000000000001";

    @Mock
    private ProductPersistencePort productRepository;
    @Mock
    private ProductSearchIndex searchIndex;
    @Mock
    private ProductSearchDocumentMapper searchDocumentMapper;
    @Mock
    private AttributeTemplatePersistencePort attributeTemplateRepository;

    @InjectMocks
    private MerchantSearchSyncListener listener;

    private Product publishedProduct() {
        Product product = Product.create("01HZPRD0000000000000000001", MERCHANT_ID, "Widget");
        product.categorize(List.of("c1"));
        product.defineVariant("sku-1", Map.of("color", "red"), Money.of(new BigDecimal("100"), "VND"));
        product.publish(ADMIN_ID);
        product.pullEvents();
        return product;
    }

    @Test
    void onSuspendedRemovesMerchantProducts() {
        when(productRepository.listByMerchant(MERCHANT_ID, OffsetPagination.of(0, 100)))
                .thenReturn(List.of(publishedProduct()));

        listener.onSuspended(new MerchantEvents.MerchantSuspended(
                MERCHANT_ID, "fraud", ADMIN_ID, Instant.now(), Instant.now()));

        verify(searchIndex).removeAll(anyList());
    }

    @Test
    void onActivatedReindexesPublishedProducts() {
        when(productRepository.listByMerchant(MERCHANT_ID, OffsetPagination.of(0, 100)))
                .thenReturn(List.of(publishedProduct()));
        when(searchDocumentMapper.toSearchDocument(any(), any())).thenReturn(mock());

        listener.onActivated(new MerchantEvents.MerchantActivated(
                MERCHANT_ID, ADMIN_ID, "ok", Instant.now(), Instant.now()));

        verify(searchIndex).indexAll(anyList());
    }

    @Test
    void onProfileUpdatedSkipsWhenProvinceUnchanged() {
        listener.onProfileUpdated(new MerchantEvents.MerchantProfileUpdated(
                MERCHANT_ID, "Name", null, null, "01", "Ha Noi", false, Instant.now()));

        verify(productRepository, never()).listByMerchant(any(), any());
    }

    private static ProductSearchDocument mock() {
        return new ProductSearchDocument("01HZPRD0000000000000000001", MERCHANT_ID, "Widget", null, null,
                List.of("c1"), List.of(), List.of(), List.of(), Map.of(), null, null, null, "PUBLISHED",
                Instant.now(), 0.0, 0L);
    }
}
