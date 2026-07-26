package com.aionn.promotion.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
