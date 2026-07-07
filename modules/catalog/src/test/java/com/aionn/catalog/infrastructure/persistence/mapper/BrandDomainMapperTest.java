package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.Brand;
import com.aionn.catalog.domain.valueobject.BrandStatus;
import com.aionn.catalog.infrastructure.persistence.entity.BrandEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BrandDomainMapperTest {

    private final BrandDomainMapper mapper = new BrandDomainMapperImpl();

    private Brand sampleBrand() {
        Instant now = Instant.now();
        return new Brand("b1", "Acme", "logo.png", "desc", BrandStatus.ACTIVE, now, now,
                List.of(new Brand.Translation("vi", "Ten", "Mo ta")));
    }

    @Test
    void toEntityMapsFieldsAndTranslations() {
        BrandEntity entity = mapper.toEntity(sampleBrand());

        assertThat(entity.getBrandId()).isEqualTo("b1");
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");
        assertThat(entity.getTranslations()).hasSize(1);
        assertThat(entity.getTranslations().get(0).getName()).isEqualTo("Ten");
    }

    @Test
    void roundTripBackToDomain() {
        Brand domain = mapper.toDomain(mapper.toEntity(sampleBrand()));

        assertThat(domain.getBrandId()).isEqualTo("b1");
        assertThat(domain.getStatus()).isEqualTo(BrandStatus.ACTIVE);
        assertThat(domain.translations()).hasSize(1);
        assertThat(domain.translations().get(0).description()).isEqualTo("Mo ta");
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
