package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.mapper.ProductSearchDocumentMapper;
import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import com.aionn.catalog.domain.event.ProductEvents;
import com.aionn.catalog.domain.model.Product;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchIndexUpdaterTest {

        private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
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
        private ProductSearchIndexUpdater updater;

        private Product publishedProduct() {
                Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
                product.categorize(List.of("c1"));
                product.defineVariant("sku-1", Map.of("color", "red"), Money.of(new BigDecimal("100"), "VND"));
                product.publish(ADMIN_ID);
                product.pullEvents();
                return product;
        }

        private ProductSearchDocument doc() {
                return new ProductSearchDocument(PRODUCT_ID, MERCHANT_ID, "Widget", null, null, List.of("c1"),
                                List.of(), List.of(), List.of(), Map.of(), null, null, null, "PUBLISHED", Instant.now(),
                                0.0, 0L);
        }

        @Test
        void onProductPublishedReindexes() {
                Product product = publishedProduct();
                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                when(searchDocumentMapper.toSearchDocument(any(), any())).thenReturn(doc());

                updater.onProductPublished(new ProductEvents.ProductPublished(
                                PRODUCT_ID, ADMIN_ID, Instant.now(), Instant.now()));

                verify(searchIndex).index(any(ProductSearchDocument.class));
        }

        @Test
        void onProductRejectedRemovesFromIndex() {
                updater.onProductRejected(new ProductEvents.ProductRejected(
                                PRODUCT_ID, ADMIN_ID, "SPAM", "bad", Instant.now()));

                verify(searchIndex).remove(PRODUCT_ID);
                verify(productRepository, never()).findById(any());
        }

        @Test
        void onProductDeactivatedRemovesFromIndex() {
                updater.onProductDeactivated(new ProductEvents.ProductDeactivated(
                                PRODUCT_ID, MERCHANT_ID, "policy", Instant.now(), Instant.now()));

                verify(searchIndex).remove(PRODUCT_ID);
        }

        @Test
        void onProductVariantDefinedReindexesWhenSearchable() {
                Product product = publishedProduct();
                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                when(searchDocumentMapper.toSearchDocument(any(), any())).thenReturn(doc());

                updater.onProductVariantDefined(new ProductEvents.ProductVariantDefined(
                                PRODUCT_ID, "sku-1", Map.of(), Instant.now()));

                verify(searchIndex).index(any(ProductSearchDocument.class));
        }

        @Test
        void onProductVariantDefinedSkipsWhenNotSearchable() {
                Product draft = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(draft));

                updater.onProductVariantDefined(new ProductEvents.ProductVariantDefined(
                                PRODUCT_ID, "sku-1", Map.of(), Instant.now()));

                verify(searchIndex, never()).index(any());
        }

        @Test
        void onProductRestoredReindexes() {
                Product product = publishedProduct();
                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                when(searchDocumentMapper.toSearchDocument(any(), any())).thenReturn(doc());

                updater.onProductRestored(new ProductEvents.ProductRestored(PRODUCT_ID, Instant.now(), Instant.now()));

                verify(searchIndex).index(any(ProductSearchDocument.class));
        }

        @Test
        void onProductEmergencyTakedownRemoves() {
                updater.onProductEmergencyTakedown(new ProductEvents.ProductEmergencyTakedown(
                                PRODUCT_ID, ADMIN_ID, "abuse", Instant.now(), Instant.now()));
                verify(searchIndex).remove(PRODUCT_ID);
        }

        @Test
        void searchableMutationsReindex() {
                Product product = publishedProduct();
                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                when(searchDocumentMapper.toSearchDocument(any(), any())).thenReturn(doc());

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

                verify(searchIndex, times(8)).index(any(ProductSearchDocument.class));
        }

        @Test
        void buildSearchDocumentIncludesFilterableAttributes() {
                Product product = publishedProduct();
                product.defineAttributes(Map.of("color", "red"));
                product.pullEvents();
                com.aionn.catalog.domain.model.AttributeTemplate template = com.aionn.catalog.domain.model.AttributeTemplate
                                .create("t1", "c1", List.of("color"));
                when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
                when(attributeTemplateRepository.findByCategoryId("c1")).thenReturn(Optional.of(template));
                when(searchDocumentMapper.toSearchDocument(any(), any())).thenReturn(doc());

                updater.onProductAttributesDefined(new ProductEvents.ProductAttributesDefined(
                                PRODUCT_ID, Map.of("color", "red"), Instant.now()));

                verify(searchDocumentMapper).toSearchDocument(any(), org.mockito.ArgumentMatchers.argThat(
                                m -> "red".equals(m.get("color"))));
        }
}
