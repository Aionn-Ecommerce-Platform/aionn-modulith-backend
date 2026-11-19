package com.aionn.contracts.port.catalog;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogProductAttributeQueryPortTest {

    @Test
    void shortConstructorDefaultsOptionalFieldsAndCopiesCollections() {
        CatalogProductAttributeQueryPort.ProductAttributeView view =
                new CatalogProductAttributeQueryPort.ProductAttributeView(
                        "p-1", "Product", "b-1", null, null, "image",
                        BigDecimal.ONE, BigDecimal.TEN, "VND");

        assertThat(view.categoryIds()).isEmpty();
        assertThat(view.skuIds()).isEmpty();
        assertThat(view.rating()).isNull();
        assertThat(view.soldCount()).isZero();
        assertThat(view.provinceCode()).isNull();
        assertThat(view.provinceName()).isNull();
    }

    @Test
    void fullConstructorKeepsProvinceAndMetrics() {
        CatalogProductAttributeQueryPort.ProductAttributeView view =
                new CatalogProductAttributeQueryPort.ProductAttributeView(
                        "p-1", "Product", "b-1", List.of("c-1"), List.of("s-1"),
                        "image", BigDecimal.ONE, BigDecimal.TEN, "VND",
                        4.5, 12L, 34L, "48", "Da Nang");

        assertThat(view.categoryIds()).containsExactly("c-1");
        assertThat(view.skuIds()).containsExactly("s-1");
        assertThat(view.rating()).isEqualTo(4.5);
        assertThat(view.reviewCount()).isEqualTo(12L);
        assertThat(view.soldCount()).isEqualTo(34L);
        assertThat(view.provinceCode()).isEqualTo("48");
        assertThat(view.provinceName()).isEqualTo("Da Nang");
    }
}

