package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.mapper.ProductSearchDocumentMapper;
import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductSoldCounterPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.aionn.sharedkernel.testing.fixture.TestClocks.FIXED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSearchIndexWriterTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String ADMIN_ID = "01HZADM0000000000000000001";
    private static final ProductVariant.ParcelDimensions TEST_PARCEL =
            new ProductVariant.ParcelDimensions(500, BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.valueOf(5));

    @Mock
    private ProductPersistencePort productRepository;
    @Mock
    private ProductSearchIndex searchIndex;
    @Mock
    private ProductSearchDocumentMapper searchDocumentMapper;
    @Mock
    private AttributeTemplatePersistencePort attributeTemplateRepository;
    @Mock
    private ProductReviewPersistencePort reviewRepository;
    @Mock
    private ProductSoldCounterPersistencePort soldCounterRepository;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private CatalogSearchIndexWriter writer;

    @BeforeEach
    void executeTransactionsImmediately() {
        Mockito.lenient().when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0))
                        .doInTransaction(null));
    }

    @Test
    void reindexWritesTheDocument() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(publishedProduct()));
        when(searchDocumentMapper.toSearchDocument(any(), any(), anyDouble(), anyLong())).thenReturn(doc());

        writer.reindexAsync(PRODUCT_ID);

        verify(searchIndex).index(any(ProductSearchDocument.class));
    }

    @Test
    void conditionalReindexSkipsProductsThatAreNotSearchable() {
        Product draft = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", FIXED);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(draft));

        writer.reindexIfSearchableAsync(PRODUCT_ID);

        verify(searchIndex, never()).index(any());
    }

    @Test
    void removalDeletesTheDocument() {
        writer.removeAsync(PRODUCT_ID);

        verify(searchIndex).remove(PRODUCT_ID);
    }

    @Test
    void merchantReindexIndexesEveryPublishedProductPage() {
        when(productRepository.listByMerchant(MERCHANT_ID, OffsetPagination.of(0, 100)))
                .thenReturn(List.of(publishedProduct()));
        when(searchDocumentMapper.toSearchDocument(any(), any(), anyDouble(), anyLong())).thenReturn(doc());

        writer.reindexMerchantAsync(MERCHANT_ID);

        verify(searchIndex).indexAll(any());
    }

    @Test
    void merchantRemovalCollectsProductIds() {
        when(productRepository.listByMerchant(MERCHANT_ID, OffsetPagination.of(0, 100)))
                .thenReturn(List.of(publishedProduct()));

        writer.removeMerchantAsync(MERCHANT_ID);

        verify(searchIndex).removeAll(List.of(PRODUCT_ID));
    }

    @Test
    void documentIncludesFilterableAttributes() {
        Product product = publishedProduct();
        product.defineAttributes(Map.of("color", "red"), FIXED);
        product.pullEvents();
        AttributeTemplate template = AttributeTemplate.create("t1", "c1", List.of("color"), FIXED);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(attributeTemplateRepository.findByCategoryId("c1")).thenReturn(Optional.of(template));
        when(searchDocumentMapper.toSearchDocument(any(), any(), anyDouble(), anyLong())).thenReturn(doc());

        writer.reindexAsync(PRODUCT_ID);

        verify(searchDocumentMapper).toSearchDocument(any(), ArgumentMatchers.argThat(
                attributes -> "red".equals(attributes.get("color"))), anyDouble(), anyLong());
    }

    private Product publishedProduct() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", FIXED);
        product.categorize(List.of("c1"), FIXED);
        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("100"), "VND"), TEST_PARCEL, FIXED);
        product.publish(ADMIN_ID, FIXED);
        product.pullEvents();
        return product;
    }

    private ProductSearchDocument doc() {
        return new ProductSearchDocument(PRODUCT_ID, MERCHANT_ID, "Widget", null, null, List.of("c1"),
                List.of(), List.of(), List.of(), Map.of(), null, null, null, null, "PUBLISHED", Instant.now(),
                0.0, 0L);
    }
}
