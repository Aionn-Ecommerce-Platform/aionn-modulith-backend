package com.aionn.catalog.domain.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSlugTest {

    @Test
    void removesVietnameseDiacriticsAndPunctuation() {
        assertThat(ProductSlug.generate("Khung ảnh cây gia đình lưu giữ kỷ niệm"))
                .isEqualTo("khung-anh-cay-gia-dinh-luu-giu-ky-niem");
    }

    @Test
    void handlesDStrokeAndUppercase() {
        assertThat(ProductSlug.generate("Đầm nữ thanh lịch"))
                .isEqualTo("dam-nu-thanh-lich");
    }

    @Test
    void fallsBackWhenNameHasNoUsableCharacters() {
        assertThat(ProductSlug.generate("!!!")).isEqualTo("san-pham");
        assertThat(ProductSlug.generate(null)).isEqualTo("san-pham");
    }
}
