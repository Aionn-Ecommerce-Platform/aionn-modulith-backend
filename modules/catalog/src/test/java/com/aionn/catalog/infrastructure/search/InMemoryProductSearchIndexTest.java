package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryProductSearchIndexTest {

    private final InMemoryProductSearchIndex index = new InMemoryProductSearchIndex();

    private Product product(String productId, String name, List<String> tags, String description) {
        return new Product(
                productId,
                "merchant-1",
                name,
                null,
                List.of("cat-1"),
                List.of(),
                tags,
                List.of(),
                Map.of(),
                List.of(new ProductVariant("sku-1", Map.of("color", "red"),
                        Money.of(new BigDecimal("10"), "VND"))),
                description,
                ProductStatus.PUBLISHED,
                Instant.now(),
                Instant.now(),
                List.of());
    }

    @Test
    void indexAndSearchByName() {
        index.index(product("p1", "Red Widget", List.of(), null));
        index.index(product("p2", "Blue Gadget", List.of(), null));

        List<String> results = index.searchIds("widget", OffsetPagination.of(0, 10));

        assertThat(results).containsExactly("p1");
    }

    @Test
    void searchByTag() {
        index.index(product("p1", "Alpha", List.of("premium", "featured"), null));
        index.index(product("p2", "Beta", List.of("standard"), null));

        assertThat(index.searchIds("premium", OffsetPagination.of(0, 10))).containsExactly("p1");
    }

    @Test
    void searchByAiDescription() {
        index.index(product("p1", "Alpha", List.of(), "Handmade leather bag"));
        index.index(product("p2", "Beta", List.of(), "Plastic bottle"));

        assertThat(index.searchIds("leather", OffsetPagination.of(0, 10))).containsExactly("p1");
    }

    @Test
    void emptyKeywordReturnsAllPaged() {
        index.index(product("p1", "A", List.of(), null));
        index.index(product("p2", "B", List.of(), null));
        index.index(product("p3", "C", List.of(), null));

        List<String> firstPage = index.searchIds("", OffsetPagination.of(0, 2));
        List<String> secondPage = index.searchIds("", OffsetPagination.of(1, 2));

        assertThat(firstPage).hasSize(2);
        assertThat(secondPage).hasSize(1);
    }

    @Test
    void countMatchesReturnsExactCount() {
        index.index(product("p1", "Red Widget", List.of(), null));
        index.index(product("p2", "Red Bag", List.of(), null));
        index.index(product("p3", "Blue Gadget", List.of(), null));

        assertThat(index.countMatches("red")).isEqualTo(2L);
        assertThat(index.countMatches("nothing")).isZero();
        assertThat(index.countMatches("")).isEqualTo(3L);
    }

    @Test
    void deleteRemovesFromIndex() {
        index.index(product("p1", "Widget", List.of(), null));

        index.delete("p1");

        assertThat(index.searchIds("widget", OffsetPagination.of(0, 10))).isEmpty();
        assertThat(index.countMatches("widget")).isZero();
    }

    @Test
    void reindexingUpdatesEntry() {
        index.index(product("p1", "Widget", List.of(), null));
        index.index(product("p1", "Gadget", List.of(), null));

        assertThat(index.searchIds("widget", OffsetPagination.of(0, 10))).isEmpty();
        assertThat(index.searchIds("gadget", OffsetPagination.of(0, 10))).containsExactly("p1");
    }
}
