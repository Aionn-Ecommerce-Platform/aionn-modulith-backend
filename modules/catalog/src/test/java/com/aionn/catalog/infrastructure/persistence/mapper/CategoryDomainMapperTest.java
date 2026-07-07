package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.Category;
import com.aionn.catalog.infrastructure.persistence.entity.CategoryEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryDomainMapperTest {

    private final CategoryDomainMapper mapper = new CategoryDomainMapperImpl();

    private Category sampleCategory() {
        Instant now = Instant.now();
        return new Category("cat1", "parent1", "Electronics", "electronics", "icon.png",
                true, now, now, null,
                List.of(new Category.Translation("vi", "Điện tử")));
    }

    @Test
    void toEntityMapsFieldsAndTranslations() {
        CategoryEntity entity = mapper.toEntity(sampleCategory());

        assertThat(entity.getCategoryId()).isEqualTo("cat1");
        assertThat(entity.getParentId()).isEqualTo("parent1");
        assertThat(entity.getTranslations()).hasSize(1);
        assertThat(entity.getTranslations().get(0).getName()).isEqualTo("Điện tử");
    }

    @Test
    void roundTripBackToDomain() {
        Category domain = mapper.toDomain(mapper.toEntity(sampleCategory()));

        assertThat(domain.getCategoryId()).isEqualTo("cat1");
        assertThat(domain.getSlug()).isEqualTo("electronics");
        assertThat(domain.isActive()).isTrue();
        assertThat(domain.translations()).hasSize(1);
        assertThat(domain.translations().get(0).name()).isEqualTo("Điện tử");
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
