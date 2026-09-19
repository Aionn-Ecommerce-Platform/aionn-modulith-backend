package com.aionn.ucp.application.catalog;

import com.aionn.sharedkernel.integration.port.catalog.CatalogQueryPort;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogLookupRequest;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogLookupResponse;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogModels.*;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogSearchRequest;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogSearchResponse;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpProductDetailRequest;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpProductDetailResponse;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.aionn.ucp.infrastructure.schema.PinnedUcpSchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UcpCatalogApplicationServiceTest {

    @Mock
    private CatalogQueryPort catalogQueryPort;

    private PinnedUcpSchemaValidator schemaValidator;
    private UcpCatalogApplicationService service;

    @BeforeEach
    void setUp() {
        schemaValidator = new PinnedUcpSchemaValidator();
        service = new UcpCatalogApplicationService(catalogQueryPort, schemaValidator);
    }

    private CatalogQueryPort.ProductView sampleProduct(String productId, String name, BigDecimal minPrice,
            BigDecimal maxPrice) {
        CatalogQueryPort.VariantView v1 = new CatalogQueryPort.VariantView(
                productId + "-sku-1",
                name + " - Blue / S",
                minPrice,
                "USD",
                true,
                Map.of("Color", "Blue", "Size", "S"));

        CatalogQueryPort.VariantView v2 = new CatalogQueryPort.VariantView(
                productId + "-sku-2",
                name + " - Red / M",
                maxPrice,
                "USD",
                false,
                Map.of("Color", "Red", "Size", "M"));

        return new CatalogQueryPort.ProductView(
                productId,
                name,
                "Description for " + name,
                List.of("https://example.com/img1.jpg"),
                List.of(v1, v2));
    }

    @Test
    void searchCatalogReturnsMatchingProductsWithPaginationAndValidSchema() {
        CatalogQueryPort.ProductView p1 = sampleProduct("prod-1", "Running Shoes", BigDecimal.valueOf(80.00),
                BigDecimal.valueOf(100.00));
        when(catalogQueryPort.search(any())).thenReturn(List.of(p1));

        UcpCatalogSearchRequest request = new UcpCatalogSearchRequest(
                "shoes",
                new UcpSearchFiltersDto(null, new UcpPriceFilterDto(5000L, 12000L)),
                new UcpPaginationRequestDto(10, null),
                null, null, null);

        UcpCatalogSearchResponse response = service.searchCatalog(request);

        assertThat(response).isNotNull();
        assertThat(response.ucp().version()).isEqualTo("2026-08-25");
        assertThat(response.products()).hasSize(1);

        UcpProductDto product = response.products().get(0);
        assertThat(product.id()).isEqualTo("prod-1");
        assertThat(product.title()).isEqualTo("Running Shoes");
        assertThat(product.description().plain()).isEqualTo("Description for Running Shoes");
        assertThat(product.priceRange().min().amount()).isEqualTo(8000L);
        assertThat(product.priceRange().max().amount()).isEqualTo(10000L);
        assertThat(product.variants()).hasSize(2);
        assertThat(product.variants().get(0).id()).isEqualTo("prod-1-sku-1");
        assertThat(product.variants().get(0).availability().available()).isTrue();
        assertThat(product.variants().get(0).availability().status()).isEqualTo("in_stock");
        assertThat(product.variants().get(1).availability().available()).isFalse();
        assertThat(product.variants().get(1).availability().status()).isEqualTo("out_of_stock");

        assertThat(response.pagination().hasNextPage()).isFalse();
        assertThat(response.pagination().totalCount()).isEqualTo(1);

        ArgumentCaptor<CatalogQueryPort.SearchCriteria> captor = ArgumentCaptor
                .forClass(CatalogQueryPort.SearchCriteria.class);
        verify(catalogQueryPort).search(captor.capture());
        assertThat(captor.getValue().query()).isEqualTo("shoes");
        assertThat(captor.getValue().limit()).isEqualTo(10);
        assertThat(captor.getValue().minPrice()).isEqualByComparingTo("50.00");
        assertThat(captor.getValue().maxPrice()).isEqualByComparingTo("120.00");
    }

    @Test
    void searchCatalogWithEmptyResultsConformsToSchema() {
        when(catalogQueryPort.search(any())).thenReturn(List.of());

        UcpCatalogSearchResponse response = service
                .searchCatalog(new UcpCatalogSearchRequest("nonexistent", null, null, null, null, null));

        assertThat(response.products()).isEmpty();
        assertThat(response.pagination().hasNextPage()).isFalse();
        assertThat(response.pagination().cursor()).isNull();
    }

    @Test
    void lookupCatalogResolvesProductsWithExactAndFeaturedInputs() {
        CatalogQueryPort.ProductView p1 = sampleProduct("prod-1", "Product One", BigDecimal.valueOf(50.00),
                BigDecimal.valueOf(60.00));
        CatalogQueryPort.LookupResult result = new CatalogQueryPort.LookupResult(
                List.of(p1),
                List.of("unknown-sku-99"));
        when(catalogQueryPort.lookupByProductOrSkuIds(any())).thenReturn(result);

        UcpCatalogLookupRequest request = new UcpCatalogLookupRequest(
                List.of("prod-1", "prod-1-sku-1", "unknown-sku-99"),
                null, null, null, null);

        UcpCatalogLookupResponse response = service.lookupCatalog(request);

        assertThat(response.products()).hasSize(1);
        UcpProductDto product = response.products().get(0);
        assertThat(product.variants()).hasSize(2);

        // sku-1 matched requested id "prod-1-sku-1" directly -> "exact"
        UcpVariantDto v1 = product.variants().get(0);
        assertThat(v1.inputs()).extracting(UcpInputCorrelationDto::match).contains("exact");

        // Warning message should be present for unknown-sku-99
        assertThat(response.messages()).hasSize(1);
        assertThat(response.messages().get(0).code()).isEqualTo("item_not_found");
        assertThat(response.messages().get(0).content()).contains("unknown-sku-99");
    }

    @Test
    void lookupCatalogThrowsBadRequestWhenIdsEmpty() {
        assertThatThrownBy(() -> service.lookupCatalog(new UcpCatalogLookupRequest(List.of(), null, null, null, null)))
                .isInstanceOf(UcpProtocolException.class)
                .satisfies(ex -> {
                    UcpProtocolException ucpEx = (UcpProtocolException) ex;
                    assertThat(ucpEx.getStatusCode()).isEqualTo(400);
                    assertThat(ucpEx.getErrorCode()).isEqualTo("invalid_request");
                });

        assertThatThrownBy(() -> service.lookupCatalog(null))
                .isInstanceOf(UcpProtocolException.class)
                .satisfies(ex -> assertThat(((UcpProtocolException) ex).getStatusCode()).isEqualTo(400));
    }

    @Test
    void getProductReturnsProductDetailWithValidSchema() {
        CatalogQueryPort.ProductView p1 = sampleProduct("prod-1", "Single Item", BigDecimal.valueOf(25.00),
                BigDecimal.valueOf(25.00));
        when(catalogQueryPort.findByProductOrSkuId("prod-1")).thenReturn(Optional.of(p1));

        UcpProductDetailRequest request = new UcpProductDetailRequest(
                "prod-1",
                List.of(new UcpSelectedOptionDto("Color", "Blue", null)),
                null, null, null, null, null);

        UcpProductDetailResponse response = service.getProduct(request);

        assertThat(response.product()).isNotNull();
        assertThat(response.product().id()).isEqualTo("prod-1");
        assertThat(response.product().selected()).hasSize(1);
        assertThat(response.product().selected().get(0).name()).isEqualTo("Color");
        assertThat(response.product().selected().get(0).label()).isEqualTo("Blue");
    }

    @Test
    void getProductThrowsNotFoundWhenMissing() {
        when(catalogQueryPort.findByProductOrSkuId("prod-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service
                .getProduct(new UcpProductDetailRequest("prod-missing", null, null, null, null, null, null)))
                .isInstanceOf(UcpProtocolException.class)
                .satisfies(ex -> {
                    UcpProtocolException ucpEx = (UcpProtocolException) ex;
                    assertThat(ucpEx.getStatusCode()).isEqualTo(404);
                    assertThat(ucpEx.getErrorCode()).isEqualTo("item_not_found");
                });
    }

    @Test
    void getProductThrowsBadRequestWhenIdBlank() {
        assertThatThrownBy(
                () -> service.getProduct(new UcpProductDetailRequest("   ", null, null, null, null, null, null)))
                .isInstanceOf(UcpProtocolException.class)
                .satisfies(ex -> assertThat(((UcpProtocolException) ex).getStatusCode()).isEqualTo(400));
    }
}
