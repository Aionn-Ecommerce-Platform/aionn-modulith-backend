package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.analytics.result.ProductAnalyticsResult;
import com.aionn.catalog.application.dto.product.command.TrackProductViewCommand;
import com.aionn.catalog.application.dto.product.query.GetPersonalizedProductsQuery;
import com.aionn.catalog.application.dto.product.query.GetPopularProductsQuery;
import com.aionn.catalog.application.dto.product.query.GetRelatedProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchResult;
import com.aionn.catalog.application.port.out.analytics.ProductAnalyticsQueryPort;
import com.aionn.catalog.application.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQueryUseCasesTest {

    @Mock
    private ProductService productService;

    private ProductResult sample() {
        return new ProductResult("p1", "m1", "Widget", null, List.of(), List.of(), List.of(), Map.of(),
                List.of(), null, "PUBLISHED", java.time.Instant.now(), java.time.Instant.now());
    }

    @Test
    void getRelatedDelegates() {
        when(productService.getRelatedProducts("p1", 5)).thenReturn(List.of(sample()));
        List<ProductResult> result = new GetRelatedProductsUseCase(productService)
                .execute(new GetRelatedProductsQuery("p1", 5));
        assertThat(result).hasSize(1);
    }

    @Test
    void getPopularDelegates() {
        when(productService.getPopularProducts(5)).thenReturn(List.of(sample()));
        List<ProductResult> result = new GetPopularProductsUseCase(productService)
                .execute(new GetPopularProductsQuery(5));
        assertThat(result).hasSize(1);
    }

    @Test
    void getPersonalizedDelegates() {
        when(productService.getPersonalizedProducts("u1", List.of("c1"), List.of("b1"), 5))
                .thenReturn(List.of(sample()));
        List<ProductResult> result = new GetPersonalizedProductsUseCase(productService)
                .execute(new GetPersonalizedProductsQuery("u1", List.of("c1"), List.of("b1"), 5));
        assertThat(result).hasSize(1);
    }

    @Test
    void trackViewDelegates() {
        new TrackProductViewUseCase(productService).execute(new TrackProductViewCommand("p1", "u1"));
        verify(productService).trackProductView("p1", "u1");
    }

    @Test
    void searchCatalogDelegates() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, null, null, List.of(), List.of(),
                null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE, 0, 20);
        ProductSearchResult expected = ProductSearchResult.of(
                new com.aionn.catalog.application.dto.common.PageResult<>(List.of(), 0, 20, 0));
        when(productService.searchCatalog(criteria)).thenReturn(expected);
        assertThat(new SearchProductCatalogUseCase(productService).execute(criteria)).isEqualTo(expected);
    }

    @Test
    void analyticsDelegatesToQueryPort() {
        ProductAnalyticsQueryPort port = mock(ProductAnalyticsQueryPort.class);
        ProductAnalyticsResult expected = new ProductAnalyticsResult(1, 2, 3, 4, 5, 4.5, List.of());
        when(port.getProductAnalytics()).thenReturn(expected);
        assertThat(new GetProductAnalyticsUseCase(port).execute()).isEqualTo(expected);
    }
}
