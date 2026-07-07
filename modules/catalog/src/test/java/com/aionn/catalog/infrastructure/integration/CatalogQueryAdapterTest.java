package com.aionn.catalog.infrastructure.integration;

import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.CatalogQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogQueryAdapterTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String ADMIN_ID = "01HZADM0000000000000000001";

    @Mock
    private ProductPersistencePort productRepository;

    @InjectMocks
    private CatalogQueryAdapter adapter;

    private Product publishedProduct() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.categorize(List.of("01HZCAT0000000000000000001"));
        product.defineVariant("sku-1", Map.of("color", "red"), Money.of(new BigDecimal("100"), "VND"));
        product.publish(ADMIN_ID);
        product.pullEvents();
        return product;
    }

    @Test
    void searchReturnsViewsWithinPriceRange() {
        when(productRepository.searchPublished("widget", 10, 0)).thenReturn(List.of(publishedProduct()));

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10,
                        new BigDecimal("50"), new BigDecimal("150")));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(views.get(0).variants()).hasSize(1);
    }

    @Test
    void searchExcludesProductsOutsidePriceRange() {
        when(productRepository.searchPublished("widget", 10, 0)).thenReturn(List.of(publishedProduct()));

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10,
                        new BigDecimal("200"), new BigDecimal("300")));

        assertThat(views).isEmpty();
    }

    @Test
    void findByProductOrSkuIdResolvesByProductId() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(publishedProduct()));

        Optional<CatalogQueryPort.ProductView> view = adapter.findByProductOrSkuId(PRODUCT_ID);

        assertThat(view).isPresent();
        assertThat(view.get().productId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void findByProductOrSkuIdFallsBackToSkuLookup() {
        when(productRepository.findById("sku-1")).thenReturn(Optional.empty());
        when(productRepository.findAllBySkuIds(List.of("sku-1"))).thenReturn(List.of(publishedProduct()));

        Optional<CatalogQueryPort.ProductView> view = adapter.findByProductOrSkuId("sku-1");

        assertThat(view).isPresent();
    }

    @Test
    void lookupReportsNotFoundIds() {
        when(productRepository.findAllBySkuIds(List.of("sku-1", "missing")))
                .thenReturn(List.of(publishedProduct()));
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        CatalogQueryPort.LookupResult result = adapter.lookupByProductOrSkuIds(List.of("sku-1", "missing"));

        assertThat(result.products()).hasSize(1);
        assertThat(result.notFound()).containsExactly("missing");
    }

    @Test
    void searchWithoutPriceBoundsReturnsAll() {
        when(productRepository.searchPublished("widget", 10, 0)).thenReturn(List.of(publishedProduct()));

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10, null, null));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).variants().get(0).displayName()).contains("(");
    }

    @Test
    void searchExcludesTakenDownProducts() {
        Product takenDown = publishedProduct();
        takenDown.emergencyTakedown(ADMIN_ID, "abuse");
        takenDown.pullEvents();
        when(productRepository.searchPublished("widget", 10, 0)).thenReturn(List.of(takenDown));

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10, null, null));

        assertThat(views).isEmpty();
    }

    @Test
    void lookupResolvesByProductIdWhenNotSku() {
        Product product = publishedProduct();
        when(productRepository.findAllBySkuIds(List.of(PRODUCT_ID))).thenReturn(List.of());
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        CatalogQueryPort.LookupResult result = adapter.lookupByProductOrSkuIds(List.of(PRODUCT_ID));

        assertThat(result.products()).hasSize(1);
        assertThat(result.notFound()).isEmpty();
    }

    @Test
    void findByProductOrSkuIdReturnsEmptyWhenUnknown() {
        when(productRepository.findById("nope")).thenReturn(Optional.empty());
        when(productRepository.findAllBySkuIds(List.of("nope"))).thenReturn(List.of());

        assertThat(adapter.findByProductOrSkuId("nope")).isEmpty();
    }

    @Test
    void searchIncludesWhenPriceMinOnlySatisfied() {
        when(productRepository.searchPublished("widget", 10, 0)).thenReturn(List.of(publishedProduct()));

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10, new BigDecimal("50"), null));

        assertThat(views).hasSize(1);
    }

    @Test
    void searchExcludesWhenAboveMaxOnly() {
        when(productRepository.searchPublished("widget", 10, 0)).thenReturn(List.of(publishedProduct()));

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10, null, new BigDecimal("80")));

        assertThat(views).isEmpty();
    }
}
