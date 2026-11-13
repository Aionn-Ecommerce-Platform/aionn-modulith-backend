package com.aionn.catalog.infrastructure.integration;

import com.aionn.catalog.domain.model.ProductVariant;

import com.aionn.catalog.infrastructure.integration.catalog.CatalogQueryAdapter;

import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.domain.model.Product;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.contracts.port.catalog.CatalogQueryPort;
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
import static com.aionn.sharedkernel.testing.fixture.TestClocks.FIXED;

@ExtendWith(MockitoExtension.class)
class CatalogQueryAdapterTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String ADMIN_ID = "01HZADM0000000000000000001";
    private static final ProductVariant.ParcelDimensions TEST_PARCEL = new ProductVariant.ParcelDimensions(500, BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.valueOf(5));

    @Mock
    private ProductPersistencePort productPersistencePort;
    @Mock
    private MerchantPersistencePort merchantPersistencePort;

    @InjectMocks
    private CatalogQueryAdapter adapter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        Merchant merchant = Merchant.register(MERCHANT_ID, "owner-1", "Acme", BigDecimal.valueOf(0.05), FIXED);
        merchant.activate(ADMIN_ID, "test", FIXED);
        org.mockito.Mockito.lenient().when(merchantPersistencePort.findById(MERCHANT_ID))
                .thenReturn(Optional.of(merchant));
    }

    private Product publishedProduct() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", FIXED);
        product.categorize(List.of("01HZCAT0000000000000000001"), FIXED);
        product.defineVariant("sku-1", Map.of("color", "red"), Money.of(new BigDecimal("100"), "VND"), TEST_PARCEL, FIXED);
        product.publish(ADMIN_ID, FIXED);
        product.pullEvents();
        return product;
    }

    @Test
    void searchReturnsViewsWithinPriceRange() {
        // Price bounds are pushed into the persistence query (cheapest-variant
        // window) so offset paging runs on an already-filtered set. The adapter
        // over-fetches limit+1 as the next-page signal.
        when(productPersistencePort.searchPublished("widget", new BigDecimal("50"), new BigDecimal("150"), 11, 0))
                .thenReturn(List.of(publishedProduct()));

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10,
                        new BigDecimal("50"), new BigDecimal("150")));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(views.get(0).variants()).hasSize(1);
    }

    @Test
    void searchExcludesProductsOutsidePriceRange() {
        // Exclusion is enforced by the SQL price predicate; an out-of-window query
        // returns nothing to map.
        when(productPersistencePort.searchPublished("widget", new BigDecimal("200"), new BigDecimal("300"), 11, 0))
                .thenReturn(List.of());

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10,
                        new BigDecimal("200"), new BigDecimal("300")));

        assertThat(views).isEmpty();
    }

    @Test
    void findByProductOrSkuIdResolvesByProductId() {
        when(productPersistencePort.findById(PRODUCT_ID)).thenReturn(Optional.of(publishedProduct()));

        Optional<CatalogQueryPort.ProductView> view = adapter.findByProductOrSkuId(PRODUCT_ID);

        assertThat(view).isPresent();
        assertThat(view.get().productId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void findByProductOrSkuIdFallsBackToSkuLookup() {
        when(productPersistencePort.findById("sku-1")).thenReturn(Optional.empty());
        when(productPersistencePort.findAllBySkuIds(List.of("sku-1"))).thenReturn(List.of(publishedProduct()));

        Optional<CatalogQueryPort.ProductView> view = adapter.findByProductOrSkuId("sku-1");

        assertThat(view).isPresent();
    }

    @Test
    void lookupReportsNotFoundIds() {
        when(productPersistencePort.findAllBySkuIds(List.of("sku-1", "missing")))
                .thenReturn(List.of(publishedProduct()));
        when(productPersistencePort.findById("missing")).thenReturn(Optional.empty());

        CatalogQueryPort.LookupResult result = adapter.lookupByProductOrSkuIds(List.of("sku-1", "missing"));

        assertThat(result.products()).hasSize(1);
        assertThat(result.notFound()).containsExactly("missing");
    }

    @Test
    void searchWithoutPriceBoundsReturnsAll() {
        when(productPersistencePort.searchPublished("widget", null, null, 11, 0))
                .thenReturn(List.of(publishedProduct()));

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10, null, null));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).variants().get(0).displayName()).contains("(");
    }

    @Test
    void searchExcludesTakenDownProducts() {
        Product takenDown = publishedProduct();
        takenDown.emergencyTakedown(ADMIN_ID, "abuse", FIXED);
        takenDown.pullEvents();
        when(productPersistencePort.searchPublished("widget", null, null, 11, 0)).thenReturn(List.of(takenDown));

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10, null, null));

        assertThat(views).isEmpty();
    }

    @Test
    void lookupResolvesByProductIdWhenNotSku() {
        Product product = publishedProduct();
        when(productPersistencePort.findAllBySkuIds(List.of(PRODUCT_ID))).thenReturn(List.of());
        when(productPersistencePort.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        CatalogQueryPort.LookupResult result = adapter.lookupByProductOrSkuIds(List.of(PRODUCT_ID));

        assertThat(result.products()).hasSize(1);
        assertThat(result.notFound()).isEmpty();
    }

    @Test
    void findByProductOrSkuIdReturnsEmptyWhenUnknown() {
        when(productPersistencePort.findById("nope")).thenReturn(Optional.empty());
        when(productPersistencePort.findAllBySkuIds(List.of("nope"))).thenReturn(List.of());

        assertThat(adapter.findByProductOrSkuId("nope")).isEmpty();
    }

    @Test
    void searchIncludesWhenPriceMinOnlySatisfied() {
        when(productPersistencePort.searchPublished("widget", new BigDecimal("50"), null, 11, 0))
                .thenReturn(List.of(publishedProduct()));

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10, new BigDecimal("50"), null));

        assertThat(views).hasSize(1);
    }

    @Test
    void searchExcludesWhenAboveMaxOnly() {
        // Above-max exclusion is now applied by the SQL predicate; the port returns
        // no product for the adapter to map.
        when(productPersistencePort.searchPublished("widget", null, new BigDecimal("80"), 11, 0))
                .thenReturn(List.of());

        List<CatalogQueryPort.ProductView> views = adapter.search(
                new CatalogQueryPort.SearchCriteria("widget", 10, null, new BigDecimal("80")));

        assertThat(views).isEmpty();
    }
}
