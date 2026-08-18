package com.aionn.promotion.domain.model;

import com.aionn.promotion.domain.exception.PromotionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionBannerTest {

    private static PromotionBanner banner() {
        return PromotionBanner.create("BAN_1", "Summer", "https://cdn/a.png",
                "https://shop/sale", 1, true);
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

        b.update("Winter", null, null, 5, false);

        assertThat(b.getTitle()).isEqualTo("Winter");
        assertThat(b.getImageUrl()).isEqualTo("https://cdn/a.png");
        assertThat(b.getLinkUrl()).isEqualTo("https://shop/sale");
        assertThat(b.getDisplayOrder()).isEqualTo(5);
        assertThat(b.isActive()).isFalse();
    }

    @Test
    void updateWithAllNullsKeepsEverything() {
        PromotionBanner b = banner();

        b.update(null, null, null, null, null);

        assertThat(b.getTitle()).isEqualTo("Summer");
        assertThat(b.getDisplayOrder()).isEqualTo(1);
        assertThat(b.isActive()).isTrue();
    }

    @Test
    void updateReplacesUrls() {
        PromotionBanner b = banner();

        b.update(null, "https://cdn/b.png", "https://shop/new", null, null);

        assertThat(b.getImageUrl()).isEqualTo("https://cdn/b.png");
        assertThat(b.getLinkUrl()).isEqualTo("https://shop/new");
    }

    @Test
    void createRejectsFrontendRelativeImageUrl() {
        assertThatThrownBy(() -> PromotionBanner.create(
                "BAN_1", "Summer", "/images/banners/summer.png",
                "https://shop/sale", 1, true))
                .isInstanceOfSatisfying(PromotionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PRM_502"));
    }

    @Test
    void createRejectsInsecureImageUrl() {
        assertThatThrownBy(() -> PromotionBanner.create(
                "BAN_1", "Summer", "http://cdn.example.com/summer.png",
                "https://shop/sale", 1, true))
                .isInstanceOfSatisfying(PromotionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PRM_502"));
    }

    @Test
    void updateRejectsInvalidImageUrlWithoutMutatingBanner() {
        PromotionBanner b = banner();

        assertThatThrownBy(() -> b.update(
                null, "not a URL", null, null, null))
                .isInstanceOfSatisfying(PromotionException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("PRM_502"));
        assertThat(b.getImageUrl()).isEqualTo("https://cdn/a.png");
    }
}
