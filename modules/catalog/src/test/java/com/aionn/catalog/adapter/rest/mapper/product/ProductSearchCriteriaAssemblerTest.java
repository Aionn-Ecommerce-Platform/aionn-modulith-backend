package com.aionn.catalog.adapter.rest.mapper.product;

import com.aionn.catalog.adapter.rest.dto.product.request.SearchCatalogRequest;
import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSearchCriteriaAssemblerTest {

    private final ProductSearchCriteriaAssembler assembler = new ProductSearchCriteriaAssembler();

    @Test
    void assemblesFacetsAndSort() {
        SearchCatalogRequest request = new SearchCatalogRequest(
                " widget ", "merchant-1", List.of("category-1"), List.of("brand-1"),
                BigDecimal.ONE, BigDecimal.TEN, 4.5, true, List.of("collection-1"),
                "price_asc", 2, 50,
                Map.of("attr.color", " red, blue ", "attr.size", " ", "q", "ignored"));

        ProductSearchCriteria criteria = assembler.toCriteria(request);

        assertThat(criteria.q()).isEqualTo(" widget ");
        assertThat(criteria.merchantId()).isEqualTo("merchant-1");
        assertThat(criteria.status()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(criteria.categoryIds()).containsExactly("category-1");
        assertThat(criteria.brandIds()).containsExactly("brand-1");
        assertThat(criteria.priceMin()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(criteria.priceMax()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(criteria.ratingMin()).isEqualTo(4.5);
        assertThat(criteria.onSale()).isTrue();
        assertThat(criteria.collectionIds()).containsExactly("collection-1");
        assertThat(criteria.sort()).isEqualTo(ProductSearchCriteria.Sort.PRICE_ASC);
        assertThat(criteria.page()).isEqualTo(2);
        assertThat(criteria.size()).isEqualTo(50);
        assertThat(criteria.attributes()).containsExactly(
                Map.entry("color", List.of("red", "blue")));
    }

    @Test
    void fallsBackToRelevanceForUnknownSortAndNoFacets() {
        SearchCatalogRequest request = new SearchCatalogRequest(
                "widget", null, null, null, null, null, null, null, null,
                "not-a-sort", 0, 20, null);

        ProductSearchCriteria criteria = assembler.toCriteria(request);

        assertThat(criteria.sort()).isEqualTo(ProductSearchCriteria.Sort.RELEVANCE);
        assertThat(criteria.attributes()).isEmpty();
        assertThat(criteria.categoryIds()).isEmpty();
        assertThat(criteria.brandIds()).isEmpty();
        assertThat(criteria.collectionIds()).isEmpty();
    }

    @Test
    void fallsBackToRelevanceForABlankSort() {
        SearchCatalogRequest request = new SearchCatalogRequest(
                null, null, null, null, null, null, null, null, null,
                " ", 0, 20, Map.of());

        assertThat(assembler.toCriteria(request).sort())
                .isEqualTo(ProductSearchCriteria.Sort.RELEVANCE);
    }

    @Test
    void ignoresMalformedFacetEntries() {
        Map<String, String> params = new HashMap<>();
        params.put(null, "missing-key");
        params.put("page", "2");
        params.put("attr.empty", " ");
        params.put("attr.nullValue", null);
        params.put("attr.value", "a,,b");

        SearchCatalogRequest request = new SearchCatalogRequest(
                null, null, null, null, null, null, null, null, null,
                "RELEVANCE", 0, 20, params);

        assertThat(assembler.toCriteria(request).attributes())
                .containsExactly(Map.entry("value", List.of("a", "b")));
    }
}
