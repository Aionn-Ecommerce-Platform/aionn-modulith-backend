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
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.categorize(List.of("cat-1"));
        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"));
        product.pullEvents();
        return product;
    }

    @Test
    void createInitializesAsDraft() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.pullEvents()).hasSize(1);
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Product.create(PRODUCT_ID, MERCHANT_ID, " "))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void ensureOwnedByRejectsMismatch() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");

        assertThatThrownBy(() -> product.ensureOwnedBy("other-merchant"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_FORBIDDEN.getCode());
    }

    @Test
    void defineVariantAddsSku() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.pullEvents();

        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"));

        assertThat(product.variants()).hasSize(1);
        assertThat(product.findVariant("sku-1")).isPresent();
    }

    @Test
    void defineVariantRejectsDuplicateSkuId() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"));

        assertThatThrownBy(() -> product.defineVariant("sku-1", Map.of("color", "blue"),
                Money.of(new BigDecimal("12.00"), "VND")))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_VARIANT_DUPLICATE.getCode());
    }

    @Test
    void defineVariantRejectsDuplicateAttributeCombination() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"));

        assertThatThrownBy(() -> product.defineVariant("sku-2", Map.of("color", "red"),
                Money.of(new BigDecimal("12.00"), "VND")))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_VARIANT_DUPLICATE.getCode());
    }

    @Test
    void changeVariantPriceUpdatesSku() {
        Product product = publishableProduct();

        product.changeVariantPrice("sku-1", Money.of(new BigDecimal("20.00"), "VND"));

        assertThat(product.findVariant("sku-1").orElseThrow().price().amount())
                .isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void changeVariantPriceThrowsWhenSkuNotFound() {
        Product product = publishableProduct();

        assertThatThrownBy(() -> product.changeVariantPrice("missing",
                Money.of(new BigDecimal("20.00"), "VND")))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_VARIANT_NOT_FOUND.getCode());
    }

    @Test
    void assignBrandSetsBrandId() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.pullEvents();

        product.assignBrand("brand-1");

        assertThat(product.getBrandId()).isEqualTo("brand-1");
    }

    @Test
    void categorizeRejectsEmptyList() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");

        assertThatThrownBy(() -> product.categorize(List.of()))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_CATEGORY_REQUIRED.getCode());
    }

    @Test
    void publishSetsStatusPublishedWhenRequirementsMet() {
        Product product = publishableProduct();

        product.publish("admin-1");

        assertThat(product.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
    }

    @Test
    void publishRejectsWhenNoVariants() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.categorize(List.of("cat-1"));

        assertThatThrownBy(() -> product.publish("admin-1"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_PUBLISH_REQUIREMENTS.getCode());
    }

    @Test
    void publishRejectsWhenNoCategories() {
        Product product = Product.create(PRODUCT_ID, MERCHANT_ID, "Widget");
        product.defineVariant("sku-1", Map.of("color", "red"),
                Money.of(new BigDecimal("10.00"), "VND"));

        assertThatThrownBy(() -> product.publish("admin-1"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_PUBLISH_REQUIREMENTS.getCode());
    }

    @Test
    void deactivateTransitionsToHidden() {
        Product product = publishableProduct();
        product.publish("admin-1");
        product.pullEvents();

        product.deactivate("policy");

        assertThat(product.getStatus()).isEqualTo(ProductStatus.HIDDEN);
    }

    @Test
    void restoreTransitionsBackToPublished() {
        Product product = publishableProduct();
        product.publish("admin-1");
        product.deactivate("policy");
        product.pullEvents();

        product.restore();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
    }

    @Test
    void rejectTransitionsFromPendingReview() {
        Product product = publishableProduct();
        product.submitForReview(MERCHANT_ID);
        product.pullEvents();

        product.reject("admin-1", "IMG_ISSUE", "Missing photos");

        assertThat(product.getStatus()).isEqualTo(ProductStatus.REJECTED);
    }

    @Test
    void publishFromTakedownStateThrows() {
        Product product = publishableProduct();
        product.publish("admin-1");
        product.emergencyTakedown("admin-1", "abuse");
        product.pullEvents();

        assertThatThrownBy(() -> product.publish("admin-1"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.PRODUCT_INVALID_TRANSITION.getCode());
    }
}
