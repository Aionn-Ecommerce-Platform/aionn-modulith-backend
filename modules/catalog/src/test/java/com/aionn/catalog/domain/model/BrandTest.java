package com.aionn.catalog.domain.model;

import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.valueobject.BrandStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandTest {

    private static final String BRAND_ID = "01HZBRD0000000000000000001";

    @Test
    void createInitializesAsActive() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, "desc");

        assertThat(brand.getStatus()).isEqualTo(BrandStatus.ACTIVE);
        assertThat(brand.pullEvents()).hasSize(1);
        assertThat(brand.getName()).isEqualTo("Acme");
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Brand.create(BRAND_ID, " ", null, null))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void updateAppliesAllFieldsWhenProvided() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        brand.pullEvents();

        brand.update("New Name", "https://logo", "new desc");

        assertThat(brand.getName()).isEqualTo("New Name");
        assertThat(brand.getLogoUrl()).isEqualTo("https://logo");
        assertThat(brand.getDescription()).isEqualTo("new desc");
        assertThat(brand.pullEvents()).hasSize(1);
    }

    @Test
    void updateIgnoresBlankNameAndKeepsExisting() {
        Brand brand = Brand.create(BRAND_ID, "Acme", "old", "old desc");
        brand.pullEvents();

        brand.update(" ", null, null);

        assertThat(brand.getName()).isEqualTo("Acme");
    }

    @Test
    void softDeleteSetsDeletedStatus() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        brand.pullEvents();

        brand.softDelete("policy");

        assertThat(brand.getStatus()).isEqualTo(BrandStatus.DELETED);
        assertThat(brand.pullEvents()).hasSize(1);
    }

    @Test
    void softDeleteIsIdempotent() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        brand.pullEvents();
        brand.softDelete("policy");
        brand.pullEvents();

        brand.softDelete("again");

        assertThat(brand.getStatus()).isEqualTo(BrandStatus.DELETED);
        assertThat(brand.pullEvents()).isEmpty();
    }

    @Test
    void updateOnDeletedBrandThrows() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        brand.softDelete("policy");

        assertThatThrownBy(() -> brand.update("Renamed", null, null))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.BRAND_NOT_FOUND.getCode());
    }

    @Test
    void translationsAccessorReturnsImmutableSnapshot() {
        Brand.Translation vi = new Brand.Translation("vi", "Acme VN", "Mô tả");
        Brand brand = new Brand(
                BRAND_ID, "Acme", null, "desc",
                BrandStatus.ACTIVE, Instant.now(), Instant.now(), List.of(vi));

        List<Brand.Translation> translations = brand.translations();

        assertThat(translations).containsExactly(vi);
        assertThat(translations.get(0).locale()).isEqualTo("vi");
        assertThat(translations.get(0).name()).isEqualTo("Acme VN");
        assertThat(translations.get(0).description()).isEqualTo("Mô tả");
    }
}
