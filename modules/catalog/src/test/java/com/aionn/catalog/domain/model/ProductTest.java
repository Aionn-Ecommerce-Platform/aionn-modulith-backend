package com.aionn.catalog.domain.model;

import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String MERCHANT_ID = "01HZMER0000000000000000001";

    private Product publishableProduct() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.categorize(List.of("cat-1"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.pullEvents();
        return product;
    }

    @Test
    void createInitializesAsDraft() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.pullEvents()).hasSize(1);
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Product.create(PRODUCT_ID, MERCHANT_ID, " ", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void ensureOwnedByRejectsMismatch() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThatThrownBy(() -> product.ensureOwnedBy("other-merchant"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_FORBIDDEN.getCode());
    }

    @Test
    void defineVariantAddsSku() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.pullEvents();

        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(product.variants()).hasSize(1);
        assertThat(product.findVariant("sku-1")).isPresent();
    }

    @Test
    void defineVariantRejectsDuplicateSkuId() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThatThrownBy(() -> product.defineVariant("sku-1", Map.of("color", "blue"),
                Money.of(new BigDecimal("12.00"), "VND"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_VARIANT_DUPLICATE.getCode());
    }

    @Test
    void defineVariantRejectsDuplicateAttributeCombination() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThatThrownBy(() -> product.defineVariant("sku-2", Map.of("color", "red"),
                Money.of(new BigDecimal("12.00"), "VND"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_VARIANT_DUPLICATE.getCode());
    }

    @Test
    void changeVariantPriceUpdatesSku() {
        Product product = publishableProduct();

        product.changeVariantPrice("sku-1", Money.of(new BigDecimal("20.00"), "VND"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(product.findVariant("sku-1").orElseThrow().price().amount())
                .isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void changeVariantPriceThrowsWhenSkuNotFound() {
        Product product = publishableProduct();

        assertThatThrownBy(() -> product.changeVariantPrice("missing",
                Money.of(new BigDecimal("20.00"), "VND"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_VARIANT_NOT_FOUND.getCode());
    }

    @Test
    void assignBrandSetsBrandId() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.pullEvents();

        product.assignBrand("brand-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(product.getBrandId()).isEqualTo("brand-1");
    }

    @Test
    void categorizeRejectsEmptyList() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThatThrownBy(() -> product.categorize(List.of(), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_CATEGORY_REQUIRED.getCode());
    }

    @Test
    void publishSetsStatusPublishedWhenRequirementsMet() {
        Product product = publishableProduct();

        product.publish("admin-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
    }

    @Test
    void publishRejectsWhenNoVariants() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.categorize(List.of("cat-1"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThatThrownBy(() -> product.publish("admin-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_PUBLISH_REQUIREMENTS.getCode());
    }

    @Test
    void publishRejectsWhenNoCategories() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThatThrownBy(() -> product.publish("admin-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_PUBLISH_REQUIREMENTS.getCode());
    }

    @Test
    void deactivateTransitionsToHidden() {
        Product product = publishableProduct();
        product.publish("admin-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.pullEvents();

        product.deactivate("policy", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.HIDDEN);
    }

    @Test
    void restoreTransitionsBackToPublished() {
        Product product = publishableProduct();
        product.publish("admin-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.deactivate("policy", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.pullEvents();

        product.restore(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
    }

    @Test
    void rejectTransitionsFromPendingReview() {
        Product product = publishableProduct();
        product.submitForReview(MERCHANT_ID, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.pullEvents();

        product.reject("admin-1", "IMG_ISSUE", "Missing photos", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.REJECTED);
    }

    @Test
    void publishFromTakedownStateThrows() {
        Product product = publishableProduct();
        product.publish("admin-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.emergencyTakedown("admin-1", "abuse", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        product.pullEvents();

        assertThatThrownBy(() -> product.publish("admin-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_INVALID_TRANSITION.getCode());
    }
}
