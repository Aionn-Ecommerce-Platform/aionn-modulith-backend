package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchDocument;
import com.aionn.catalog.application.port.out.search.ProductSearchIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFacetProductSearchIndexTest {

    private InMemoryFacetProductSearchIndex index;

    private static ProductSearchDocument doc(String id, String brandId, List<String> categories,
            BigDecimal priceFrom, Map<String, String> attrs, long sold, double rating, String status) {
        return new ProductSearchDocument(id, "m-1", "Widget " + id, "desc " + id, brandId, categories,
                List.of(), List.of("tag"), List.of(), attrs, priceFrom, priceFrom, "VND", status,
                Instant.now(), rating, sold);
    }

    private static ProductSearchCriteria criteria(List<String> categoryIds, List<String> brandIds,
            BigDecimal min, BigDecimal max, Map<String, List<String>> attrs, ProductSearchCriteria.Sort sort) {
        return new ProductSearchCriteria(null, null, null, categoryIds, brandIds, min, max, attrs, sort, 0, 20);
    }

    @BeforeEach
    void setUp() {
        index = new InMemoryFacetProductSearchIndex();
        index.index(doc("p1", "b1", List.of("c1"), new BigDecimal("100"), Map.of("color", "red"), 5, 4.0, "PUBLISHED"));
        index.index(doc("p2", "b2", List.of("c1", "c2"), new BigDecimal("200"), Map.of("color", "blue"), 20, 5.0,
                "PUBLISHED"));
        index.index(doc("p3", "b1", List.of("c3"), new BigDecimal("50"), Map.of("color", "red"), 1, 3.0, "HIDDEN"));
    }

    @Test
    void searchExcludesNonPublishedByDefault() {
        Optional<ProductSearchIndex.SearchHits> hits = index.search(
                criteria(List.of(), List.of(), null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE));

        assertThat(hits).isPresent();
        assertThat(hits.get().productIds()).containsExactlyInAnyOrder("p1", "p2");
    }

    @Test
    void searchFiltersByBrandAndComputesFacets() {
        Optional<ProductSearchIndex.SearchHits> hits = index.search(
                criteria(List.of(), List.of("b1"), null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE));

        assertThat(hits.get().productIds()).containsExactly("p1");
        assertThat(hits.get().brandCounts()).containsEntry("b1", 1L);
        assertThat(hits.get().categoryCounts()).containsEntry("c1", 1L);
        assertThat(hits.get().attributeCounts()).containsKey("color");
    }

    @Test
    void searchFiltersByPriceRange() {
        Optional<ProductSearchIndex.SearchHits> hits = index.search(
                criteria(List.of(), List.of(), new BigDecimal("150"), new BigDecimal("250"), Map.of(),
                        ProductSearchCriteria.Sort.RELEVANCE));

        assertThat(hits.get().productIds()).containsExactly("p2");
    }

    @Test
    void searchFiltersByAttributes() {
        Optional<ProductSearchIndex.SearchHits> hits = index.search(
                criteria(List.of(), List.of(), null, null, Map.of("color", List.of("blue")),
                        ProductSearchCriteria.Sort.RELEVANCE));

        assertThat(hits.get().productIds()).containsExactly("p2");
    }

    @Test
    void searchSortsByPriceAscending() {
        Optional<ProductSearchIndex.SearchHits> hits = index.search(
                criteria(List.of(), List.of(), null, null, Map.of(), ProductSearchCriteria.Sort.PRICE_ASC));

        assertThat(hits.get().productIds()).containsExactly("p1", "p2");
    }

    @Test
    void searchSortsByBestSeller() {
        Optional<ProductSearchIndex.SearchHits> hits = index.search(
                criteria(List.of(), List.of(), null, null, Map.of(), ProductSearchCriteria.Sort.BEST_SELLER));

        assertThat(hits.get().productIds()).containsExactly("p2", "p1");
    }

    @Test
    void searchByTextMatchesName() {
        ProductSearchCriteria textCriteria = new ProductSearchCriteria("Widget p2", null, null,
                List.of(), List.of(), null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE, 0, 20);

        Optional<ProductSearchIndex.SearchHits> hits = index.search(textCriteria);

        assertThat(hits.get().productIds()).containsExactly("p2");
    }

    @Test
    void searchFiltersByRatingMin() {
        ProductSearchCriteria ratingCriteria = new ProductSearchCriteria(null, null, null,
                List.of(), List.of(), null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE, 0, 20, 4.5);

        Optional<ProductSearchIndex.SearchHits> hits = index.search(ratingCriteria);

        assertThat(hits.get().productIds()).containsExactly("p2");
    }

    @Test
    void removeAndRemoveAllDropDocuments() {
        index.remove("p1");
        index.removeAll(List.of("p2"));

        Optional<ProductSearchIndex.SearchHits> hits = index.search(
                criteria(List.of(), List.of(), null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE));

        assertThat(hits.get().productIds()).isEmpty();
    }

    @Test
    void indexAllAddsDocuments() {
        InMemoryFacetProductSearchIndex fresh = new InMemoryFacetProductSearchIndex();
        fresh.indexAll(List.of(
                doc("x1", "b9", List.of("c9"), new BigDecimal("10"), Map.of(), 0, 0.0, "PUBLISHED")));

        Optional<ProductSearchIndex.SearchHits> hits = fresh.search(
                criteria(List.of(), List.of(), null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE));

        assertThat(hits.get().totalHits()).isEqualTo(1L);
    }

    @Test
    void searchFiltersByMerchantId() {
        index.index(doc("p9", "b1", List.of("c1"), new BigDecimal("100"), Map.of(), 0, 4.0, "PUBLISHED"));
        ProductSearchCriteria c = new ProductSearchCriteria(null, "m-1", null, List.of(), List.of(),
                null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE, 0, 20);

        Optional<ProductSearchIndex.SearchHits> hits = index.search(c);

        assertThat(hits.get().productIds()).contains("p1", "p2", "p9");
    }

    @Test
    void searchExcludesOtherMerchant() {
        ProductSearchCriteria c = new ProductSearchCriteria(null, "other-merchant", null, List.of(), List.of(),
                null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE, 0, 20);

        assertThat(index.search(c).get().productIds()).isEmpty();
    }

    @Test
    void searchHonoursExplicitStatus() {
        ProductSearchCriteria c = new ProductSearchCriteria(null, null,
                com.aionn.catalog.domain.valueobject.ProductStatus.HIDDEN, List.of(), List.of(),
                null, null, Map.of(), ProductSearchCriteria.Sort.RELEVANCE, 0, 20);

        assertThat(index.search(c).get().productIds()).containsExactly("p3");
    }

    @Test
    void searchExcludesDocWithoutPriceWhenPriceFilterSet() {
        index.index(doc("noPrice", "b1", List.of("c1"), null, Map.of(), 0, 4.0, "PUBLISHED"));

        Optional<ProductSearchIndex.SearchHits> hits = index.search(
                criteria(List.of(), List.of(), new BigDecimal("1"), new BigDecimal("999"), Map.of(),
                        ProductSearchCriteria.Sort.RELEVANCE));

        assertThat(hits.get().productIds()).doesNotContain("noPrice");
    }

    @Test
    void searchWithPriceMinOnlyAndMaxOnly() {
        assertThat(index.search(criteria(List.of(), List.of(), new BigDecimal("150"), null, Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE)).get().productIds()).containsExactly("p2");
        assertThat(index.search(criteria(List.of(), List.of(), null, new BigDecimal("150"), Map.of(),
                ProductSearchCriteria.Sort.RELEVANCE)).get().productIds()).containsExactly("p1");
    }

    @Test
    void searchSortsByNewestAndPriceDesc() {
        assertThat(index.search(criteria(List.of(), List.of(), null, null, Map.of(),
                ProductSearchCriteria.Sort.PRICE_DESC)).get().productIds()).containsExactly("p2", "p1");
        assertThat(index.search(criteria(List.of(), List.of(), null, null, Map.of(),
                ProductSearchCriteria.Sort.NEWEST)).get().productIds()).hasSize(2);
    }

    @Test
    void countMatchesRespectsPagination() {
        ProductSearchCriteria firstPageSizeOne = new ProductSearchCriteria(null, null, null, List.of(), List.of(),
                null, null, Map.of(), ProductSearchCriteria.Sort.PRICE_ASC, 0, 1);

        Optional<ProductSearchIndex.SearchHits> hits = index.search(firstPageSizeOne);

        assertThat(hits.get().productIds()).containsExactly("p1");
        assertThat(hits.get().totalHits()).isEqualTo(2L);
    }

    @Test
    void searchIgnoresAttributeFilterWithEmptyValues() {
        java.util.Map<String, List<String>> attrs = new java.util.HashMap<>();
        attrs.put("color", List.of());

        Optional<ProductSearchIndex.SearchHits> hits = index.search(
                criteria(List.of(), List.of(), null, null, attrs, ProductSearchCriteria.Sort.RELEVANCE));

        assertThat(hits.get().productIds()).contains("p1", "p2");
    }
}
