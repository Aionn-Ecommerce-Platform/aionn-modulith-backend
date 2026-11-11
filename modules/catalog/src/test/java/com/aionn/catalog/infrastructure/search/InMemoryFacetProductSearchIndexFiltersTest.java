package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFacetProductSearchIndexFiltersTest {

    private InMemoryFacetProductSearchIndex index;

    @BeforeEach
    void setUp() {
        index = new InMemoryFacetProductSearchIndex();
        index.indexAll(List.of(
                doc("p1", "m-1", "Red shoe", "Comfortable running shoe", "b1",
                        List.of("c1"), List.of("col1"), List.of("sale"), new BigDecimal("90"),
                        Map.of("color", "red"), true, ProductStatus.PUBLISHED, 4.5, 10, Instant.parse("2026-07-01T00:00:00Z")),
                doc("p2", "m-1", "Blue hat", "Warm winter hat", "b2",
                        List.of("c2"), List.of(), List.of("winter"), new BigDecimal("150"),
                        Map.of("color", "blue"), false, ProductStatus.PUBLISHED, 3.0, 50, Instant.parse("2026-07-02T00:00:00Z")),
                doc("p3", "m-2", "Draft shoe", "Unreleased", null,
                        List.of("c1"), null, List.of(), null,
                        Map.of(), false, ProductStatus.DRAFT, null, 0, Instant.parse("2026-07-03T00:00:00Z"))));
    }

    @Test
    void nullInputsAreIgnored() {
        index.indexAll(null);
        index.removeAll(null);
        index.remove("p1");

        assertThat(search(null, null, null, null, null, null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE)
                .productIds()).containsExactly("p2");
    }

    @Test
    void defaultsToPublishedDocumentsOrderedByRelevance() {
        ProductSearchIndex.SearchHits hits = search(null, null, null, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE);

        assertThat(hits.productIds()).containsExactly("p1", "p2");
        assertThat(hits.totalHits()).isEqualTo(2);
        assertThat(hits.brandCounts()).containsEntry("b1", 1L).containsEntry("b2", 1L);
        assertThat(hits.categoryCounts()).containsEntry("c1", 1L).containsEntry("c2", 1L);
        assertThat(hits.attributeCounts()).containsKey("color");
        assertThat(hits.priceMin()).isEqualByComparingTo("90");
        assertThat(hits.priceMax()).isEqualByComparingTo("150");
    }

    @Test
    void filtersByMerchantIgnoringBlankValues() {
        assertThat(search(null, "m-2", null, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).isEmpty();
        assertThat(search(null, " ", null, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).containsExactly("p1", "p2");
        assertThat(search(null, null, ProductStatus.DRAFT, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).containsExactly("p3");
    }

    @Test
    void filtersByTextAcrossNameDescriptionAndTags() {
        assertThat(search("shoe", null, null, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).containsExactly("p1");
        assertThat(search("winter", null, null, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).containsExactly("p2");
        assertThat(search("missing", null, null, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).isEmpty();
    }

    @Test
    void filtersByBrandAndRejectsDocumentsWithoutABrand() {
        assertThat(search(null, null, null, List.of(), List.of("b1"), null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).containsExactly("p1");
        assertThat(search(null, null, null, List.of(), List.of("b9"), null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).isEmpty();
    }

    @Test
    void filtersByCategoryAndCollection() {
        assertThat(search(null, null, null, List.of("c2"), List.of(), null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).containsExactly("p2");
        assertThat(searchFull(null, null, null, List.of(), List.of(), null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE, null, null, List.of("col1")).productIds())
                .containsExactly("p1");
        assertThat(searchFull(null, null, null, List.of(), List.of(), null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE, null, null, List.of("col9")).productIds()).isEmpty();
    }

    @Test
    void filtersByPriceRange() {
        assertThat(search(null, null, null, null, null, new BigDecimal("100"), null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).containsExactly("p2");
        assertThat(search(null, null, null, null, null, null, new BigDecimal("100"), Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).containsExactly("p1");
    }

    @Test
    void filtersByAttributesIgnoringEmptySelections() {
        assertThat(search(null, null, null, null, null, null, null, Map.of("color", List.of("red")),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).containsExactly("p1");
        assertThat(search(null, null, null, null, null, null, null, Map.of("color", List.of()),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).containsExactly("p1", "p2");
        assertThat(search(null, null, null, null, null, null, null, Map.of("color", List.of("green")),
                ProductSearchCriteria.Sort.RELEVANCE).productIds()).isEmpty();
    }

    @Test
    void filtersByOnSaleAndRating() {
        assertThat(searchFull(null, null, null, List.of(), List.of(), null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE, null, true, List.of()).productIds())
                .containsExactly("p1");
        assertThat(searchFull(null, null, null, List.of(), List.of(), null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE, 4.0, null, List.of()).productIds())
                .containsExactly("p1");
        assertThat(searchFull(null, null, null, List.of(), List.of(), null, null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE, 1.0, null, List.of()).productIds())
                .containsExactly("p1", "p2");
    }

    @Test
    void sortsByPriceAndSalesAndRecency() {
        assertThat(search(null, null, null, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.PRICE_ASC).productIds()).containsExactly("p1", "p2");
        assertThat(search(null, null, null, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.PRICE_DESC).productIds()).containsExactly("p2", "p1");
        assertThat(search(null, null, null, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.BEST_SELLER).productIds()).containsExactly("p2", "p1");
        assertThat(search(null, null, null, null, null, null, null, Map.of(),
                ProductSearchCriteria.Sort.NEWEST).productIds()).containsExactly("p2", "p1");
    }

    @Test
    void paginatesTheMatchedDocuments() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, null, null, null, null, null, null,
                null, ProductSearchCriteria.Sort.RELEVANCE, 1, 1);

        ProductSearchIndex.SearchHits hits = index.search(criteria).orElseThrow();

        assertThat(hits.productIds()).containsExactly("p2");
        assertThat(hits.totalHits()).isEqualTo(2);
    }

    private ProductSearchIndex.SearchHits search(String q, String merchantId, ProductStatus status,
            List<String> categories, List<String> brands, BigDecimal min, BigDecimal max,
            Map<String, List<String>> attributes, ProductSearchCriteria.Sort sort) {
        ProductSearchCriteria criteria = new ProductSearchCriteria(q, merchantId, status, categories, brands,
                min, max, attributes, sort, 0, 20);
        return index.search(criteria).orElseThrow();
    }

    private ProductSearchIndex.SearchHits searchFull(String q, String merchantId, ProductStatus status,
            List<String> categories, List<String> brands, BigDecimal min, BigDecimal max,
            Map<String, List<String>> attributes, ProductSearchCriteria.Sort sort, Double ratingMin,
            Boolean onSale, List<String> collectionIds) {
        ProductSearchCriteria criteria = new ProductSearchCriteria(q, merchantId, status, categories, brands,
                min, max, attributes, sort, 0, 20, ratingMin, onSale, collectionIds);
        return index.search(criteria).orElseThrow();
    }

    private static ProductSearchDocument doc(String id, String merchantId, String name, String description,
            String brandId, List<String> categories, List<String> collections, List<String> tags,
            BigDecimal price, Map<String, String> attributes, boolean discounted, ProductStatus status,
            Double rating, long sold, Instant updatedAt) {
        return new ProductSearchDocument(id, merchantId, name, description, brandId, categories, collections,
                tags, List.of(), attributes, price, price, "VND", discounted, status.name(), updatedAt,
                rating, sold);
    }
}
