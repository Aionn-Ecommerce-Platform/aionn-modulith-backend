package com.aionn.promotion.domain.model;

import com.aionn.promotion.domain.exception.PromotionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionBannerTest {

    private static PromotionBanner banner() {
        return PromotionBanner.create("BAN_1", "Summer", "https://cdn/a.png",
                "aionn/promotion/banners/a", "https://shop/sale", 1, true);
    }

    @Test
    void createKeepsProvidedValues() {
        PromotionBanner b = banner();

        assertThat(b.getBannerId()).isEqualTo("BAN_1");
        assertThat(b.getTitle()).isEqualTo("Summer");
        assertThat(b.getImageUrl()).isEqualTo("https://cdn/a.png");
        assertThat(b.getLinkUrl()).isEqualTo("https://shop/sale");
        assertThat(b.getDisplayOrder()).isEqualTo(1);
        assertThat(b.isActive()).isTrue();
        assertThat(b.getCreatedAt()).isNull();
        assertThat(b.getUpdatedAt()).isNull();
    }

    @Test
    void updateAppliesOnlyNonNullFields() {
        PromotionBanner b = banner();

        b.update("Winter", null, null, null, 5, false);

        assertThat(b.getTitle()).isEqualTo("Winter");
        assertThat(b.getImageUrl()).isEqualTo("https://cdn/a.png");
        assertThat(b.getLinkUrl()).isEqualTo("https://shop/sale");
        assertThat(b.getDisplayOrder()).isEqualTo(5);
        assertThat(b.isActive()).isFalse();
    }

    @Test
    void updateWithAllNullsKeepsEverything() {
        PromotionBanner b = banner();

        b.update(null, null, null, null, null, null);

        assertThat(b.getTitle()).isEqualTo("Summer");
        assertThat(b.getDisplayOrder()).isEqualTo(1);
        assertThat(b.isActive()).isTrue();
    }

    @Test
    void updateReplacesUrls() {
        PromotionBanner b = banner();

        b.update(null, "https://cdn/b.png", "aionn/promotion/banners/b",
                "https://shop/new", null, null);

        assertThat(b.getImageUrl()).isEqualTo("https://cdn/b.png");
        assertThat(b.getLinkUrl()).isEqualTo("https://shop/new");
    }

    @Test
    void updateClearsOptionalLinkWhenBlank() {
        PromotionBanner b = banner();

        b.update(null, null, null, "  ", null, null);

        assertThat(b.getLinkUrl()).isNull();
    }

    @Test
    void createRejectsUnsafeLinkUrl() {
        assertThatThrownBy(() -> PromotionBanner.create(
                "BAN_1", "Summer", "https://cdn/a.png", "aionn/promotion/banners/a",
                "javascript:alert(1)", 1, true))
                .isInstanceOfSatisfying(PromotionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PRM_504"));
    }

    @Test
    void createAllowsHttpLinkForLocalDevelopment() {
        PromotionBanner banner = PromotionBanner.create(
                "BAN_1", "Local", "https://cdn/a.png", "aionn/promotion/banners/a",
                "http://localhost:3000/products", 1, true);

        assertThat(banner.getLinkUrl()).isEqualTo("http://localhost:3000/products");
    }

    @Test
    void createRejectsFrontendRelativeImageUrl() {
        assertThatThrownBy(() -> PromotionBanner.create(
                "BAN_1", "Summer", "/images/banners/summer.png",
                "aionn/promotion/banners/summer", "https://shop/sale", 1, true))
                .isInstanceOfSatisfying(PromotionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PRM_502"));
    }

    @Test
    void createRejectsInsecureImageUrl() {
        assertThatThrownBy(() -> PromotionBanner.create(
                "BAN_1", "Summer", "http://cdn.example.com/summer.png",
                "aionn/promotion/banners/summer", "https://shop/sale", 1, true))
                .isInstanceOfSatisfying(PromotionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PRM_502"));
    }

    @Test
    void createRejectsMissingCloudinaryPublicId() {
        assertThatThrownBy(() -> PromotionBanner.create(
                "BAN_1", "Summer", "https://res.cloudinary.com/demo/image/upload/banner.png",
                "", "https://shop/sale", 1, true))
                .isInstanceOfSatisfying(PromotionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PRM_503"));
    }

    @Test
    void updateRejectsInvalidImageUrlWithoutMutatingBanner() {
        PromotionBanner b = banner();

        assertThatThrownBy(() -> b.update(
                null, "not a URL", "aionn/promotion/banners/new", null, null, null))
                .isInstanceOfSatisfying(PromotionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PRM_502"));
        assertThat(b.getImageUrl()).isEqualTo("https://cdn/a.png");
    }

    @Test
    void updateRequiresUrlAndPublicIdTogether() {
        PromotionBanner b = banner();

        assertThatThrownBy(() -> b.update(
                null, "https://res.cloudinary.com/demo/image/upload/new.png",
                null, null, null, null))
                .isInstanceOfSatisfying(PromotionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PRM_503"));
        assertThat(b.getImageUrl()).isEqualTo("https://cdn/a.png");
        assertThat(b.getImagePublicId()).isEqualTo("aionn/promotion/banners/a");
    }

    @Test
    void updateDoesNotMutateTitleWhenImageUrlIsInvalid() {
        PromotionBanner b = banner();

        assertThatThrownBy(() -> b.update(
                "New Title", "invalid-url", "aionn/promotion/banners/new", null, null, null))
                .isInstanceOfSatisfying(PromotionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PRM_502"));
        assertThat(b.getTitle()).isEqualTo("Summer");
    }

    @Test
    void createsAndUpdatesWithTrimmedImageUrl() {
        PromotionBanner b = PromotionBanner.create("BAN_2", "Title", "  https://cdn/spaced.png  ",
                "aionn/promotion/banners/b", null, 1, true);
        assertThat(b.getImageUrl()).isEqualTo("https://cdn/spaced.png");

        b.update(null, "  https://cdn/updated.png  ", "aionn/promotion/banners/c", null, null, null);
        assertThat(b.getImageUrl()).isEqualTo("https://cdn/updated.png");
    }
}
