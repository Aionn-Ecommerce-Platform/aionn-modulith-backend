package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductResultMapperTest {

    private final ProductResultMapper mapper = new ProductResultMapper();

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Product product(List<Product.Translation> translations) {
        ProductVariant variant = new ProductVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("100"), "VND"));
        Instant now = Instant.now();
        return new Product("p1", "m1", "Widget", "b1",
                List.of("c1"), List.of("img1"), List.of("tag1"), List.of("col1"),
                Map.of("color", "red"), List.of(variant), "ai desc", ProductStatus.PUBLISHED,
                now, now, translations);
    }

    @Test
    void mapsBaseFieldsAndVariants() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        ProductResult result = mapper.toResult(product(List.of()));

        assertThat(result.productId()).isEqualTo("p1");
        assertThat(result.name()).isEqualTo("Widget");
        assertThat(result.status()).isEqualTo("PUBLISHED");
        assertThat(result.variants()).hasSize(1);
        assertThat(result.variants().get(0).price()).isEqualByComparingTo("100");
        assertThat(result.variants().get(0).currency()).isEqualTo("VND");
    }

    @Test
    void appliesLocalizedTranslationWhenAvailable() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        ProductResult result = mapper.toResult(
                product(List.of(new Product.Translation("vi", "Tiện ích", "mô tả vi"))));

        assertThat(result.name()).isEqualTo("Tiện ích");
        assertThat(result.aiDescription()).isEqualTo("mô tả vi");
    }

    @Test
    void nullProductReturnsNull() {
        assertThat(mapper.toResult(null)).isNull();
    }
}
