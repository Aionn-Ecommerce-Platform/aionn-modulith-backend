package com.aionn.catalog.infrastructure.persistence.adapter.product;

import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.catalog.infrastructure.persistence.entity.ProductEntity;
import com.aionn.catalog.infrastructure.persistence.mapper.ProductDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.product.ProductRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductPersistenceAdapterTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";

    @Mock
    private ProductRepository jpa;
    @Mock
    private ProductDomainMapper mapper;

    @InjectMocks
    private ProductPersistenceAdapter adapter;

    @Test
    void saveMapsThroughEntityAndBack() {
        Product domain = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        ProductEntity entity = new ProductEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.save(domain)).isSameAs(domain);
    }

    @Test
    void findByIdReturnsMappedDomain() {
        Product domain = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        ProductEntity entity = new ProductEntity();
        when(jpa.findById(PRODUCT_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(PRODUCT_ID)).contains(domain);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(jpa.findById("missing")).thenReturn(Optional.empty());
        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void findAllByIdsReturnsEmptyForEmptyInput() {
        assertThat(adapter.findAllByIds(List.of())).isEmpty();
        assertThat(adapter.findAllByIds(null)).isEmpty();
    }

    @Test
    void findAllByIdsMapsResults() {
        ProductEntity entity = new ProductEntity();
        Product domain = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        when(jpa.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findAllByIds(List.of(PRODUCT_ID))).containsExactly(domain);
    }

    @Test
    void listByMerchantAppliesStableSort() {
        ProductEntity entity = new ProductEntity();
        when(jpa.findByMerchantId(eq(MERCHANT_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(Product.create(PRODUCT_ID, MERCHANT_ID, "Widget"));

        adapter.listByMerchant(MERCHANT_ID, OffsetPagination.of(0, 10));

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(jpa).findByMerchantId(eq(MERCHANT_ID), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertThat(sort.getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(sort.getOrderFor("productId").isAscending()).isTrue();
    }

    @Test
    void countByMerchantDelegatesToJpa() {
        when(jpa.countByMerchantId(MERCHANT_ID)).thenReturn(42L);

        assertThat(adapter.countByMerchant(MERCHANT_ID)).isEqualTo(42L);
    }

    @Test
    void listByStatusMapsResults() {
        ProductEntity entity = new ProductEntity();
        when(jpa.findByStatus(eq("PUBLISHED"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        Product domain = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.listByStatus(ProductStatus.PUBLISHED, OffsetPagination.of(0, 10)))
                .containsExactly(domain);
    }

    @Test
    void countByStatusDelegatesToJpa() {
        when(jpa.countByStatus("PUBLISHED")).thenReturn(7L);

        assertThat(adapter.countByStatus(ProductStatus.PUBLISHED)).isEqualTo(7L);
    }

    @Test
    void findAllBySkuIdsMapsResults() {
        ProductEntity entity = new ProductEntity();
        Product domain = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        when(jpa.findAllBySkuIdIn(List.of("sku-1"))).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findAllBySkuIds(List.of("sku-1"))).containsExactly(domain);
    }

    @Test
    void findAllBySkuIdsReturnsEmptyForEmptyInput() {
        assertThat(adapter.findAllBySkuIds(List.of())).isEmpty();
        assertThat(adapter.findAllBySkuIds(null)).isEmpty();
    }

    @Test
    void existsByBrandIdAndStatusDelegatesToJpa() {
        when(jpa.existsByBrandIdAndStatus("brand-1", "PUBLISHED")).thenReturn(true);

        assertThat(adapter.existsByBrandIdAndStatus("brand-1", ProductStatus.PUBLISHED)).isTrue();
    }

    @Test
    void existsByCategoryIdDelegatesToJpa() {
        when(jpa.existsByCategoryId("cat-1")).thenReturn(true);

        assertThat(adapter.existsByCategoryId("cat-1")).isTrue();
    }

    @Test
    void findRelatedProductsPassesEmptyCategoryPlaceholder() {
        ProductEntity entity = new ProductEntity();
        Product domain = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        when(jpa.findRelatedProducts(PRODUCT_ID, "brand-1", List.of(""), 5)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findRelatedProducts(PRODUCT_ID, "brand-1", List.of(), 5)).containsExactly(domain);
    }

    @Test
    void findRelatedProductsForwardsCategories() {
        when(jpa.findRelatedProducts(PRODUCT_ID, null, List.of("c1"), 5)).thenReturn(List.of());

        assertThat(adapter.findRelatedProducts(PRODUCT_ID, null, List.of("c1"), 5)).isEmpty();
    }

    @Test
    void findPopularProductsMapsResults() {
        ProductEntity entity = new ProductEntity();
        Product domain = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        when(jpa.findPopularProducts(5)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findPopularProducts(5)).containsExactly(domain);
    }

    @Test
    void findPersonalizedProductsUsesPlaceholdersWhenEmpty() {
        when(jpa.findPersonalizedProducts(List.of(""), List.of(""), 5)).thenReturn(List.of());

        assertThat(adapter.findPersonalizedProducts(List.of(), List.of(), 5)).isEmpty();
    }

    @Test
    void findPersonalizedProductsForwardsValues() {
        ProductEntity entity = new ProductEntity();
        Product domain = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        when(jpa.findPersonalizedProducts(List.of("c1"), List.of("b1"), 5)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findPersonalizedProducts(List.of("c1"), List.of("b1"), 5)).containsExactly(domain);
    }

    @Test
    void findPublishedClampsLimitAndOffset() {
        ProductEntity entity = new ProductEntity();
        Product domain = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        when(jpa.findPublished(PageRequest.of(0, 1))).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findPublished(0, -5)).containsExactly(domain);
    }

    @Test
    void countPublishedDelegates() {
        when(jpa.countPublished()).thenReturn(9L);

        assertThat(adapter.countPublished()).isEqualTo(9L);
    }

    @Test
    void searchPublishedNormalizesBlankQueryToNull() {
        when(jpa.searchPublished(null, 20, 0)).thenReturn(List.of());

        assertThat(adapter.searchPublished("   ", 20, 0)).isEmpty();
    }

    @Test
    void searchPublishedTrimsQuery() {
        ProductEntity entity = new ProductEntity();
        Product domain = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        when(jpa.searchPublished("widget", 20, 0)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.searchPublished("  widget  ", 20, 0)).containsExactly(domain);
    }

    @Test
    void countSearchPublishedNormalizesQuery() {
        when(jpa.countSearchPublished(null)).thenReturn(3L);

        assertThat(adapter.countSearchPublished("")).isEqualTo(3L);
    }

    @Test
    void findByIdsPreserveOrderReturnsEmptyForEmptyInput() {
        assertThat(adapter.findByIdsPreserveOrder(List.of())).isEmpty();
        assertThat(adapter.findByIdsPreserveOrder(null)).isEmpty();
    }

    @Test
    void findByIdsPreserveOrderKeepsRequestedOrder() {
        ProductEntity e1 = new ProductEntity();
        ProductEntity e2 = new ProductEntity();
        Product p1 = Product.create("01HZPRD0000000000000000001", MERCHANT_ID, "A");
        Product p2 = Product.create("01HZPRD0000000000000000002", MERCHANT_ID, "B");
        when(jpa.findAllById(List.of(p2.getProductId(), p1.getProductId()))).thenReturn(List.of(e1, e2));
        when(mapper.toDomain(e1)).thenReturn(p1);
        when(mapper.toDomain(e2)).thenReturn(p2);

        assertThat(adapter.findByIdsPreserveOrder(List.of(p2.getProductId(), p1.getProductId())))
                .containsExactly(p2, p1);
    }
}
