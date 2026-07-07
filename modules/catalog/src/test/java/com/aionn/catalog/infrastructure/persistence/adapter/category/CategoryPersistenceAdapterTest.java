package com.aionn.catalog.infrastructure.persistence.adapter.category;

import com.aionn.catalog.domain.model.Category;
import com.aionn.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.aionn.catalog.infrastructure.persistence.mapper.CategoryDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.category.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryPersistenceAdapterTest {

    private static final String CATEGORY_ID = "01HZCAT0000000000000000001";

    @Mock
    private CategoryRepository jpa;
    @Mock
    private CategoryDomainMapper mapper;

    @InjectMocks
    private CategoryPersistenceAdapter adapter;

    @Test
    void saveMapsThroughEntityAndBack() {
        Category domain = Category.create(CATEGORY_ID, null, "A", "a");
        CategoryEntity entity = new CategoryEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.save(domain)).isSameAs(domain);
    }

    @Test
    void findByIdReturnsMappedDomainWhenPresent() {
        CategoryEntity entity = new CategoryEntity();
        Category domain = Category.create(CATEGORY_ID, null, "A", "a");
        when(jpa.findById(CATEGORY_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(CATEGORY_ID)).contains(domain);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(jpa.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void existsByParentAndNameDelegatesToJpaIgnoreCase() {
        when(jpa.existsByParentIdAndNameIgnoreCase(null, "A")).thenReturn(true);

        assertThat(adapter.existsByParentAndName(null, "A")).isTrue();
    }

    @Test
    void existsBySlugDelegatesToJpa() {
        when(jpa.existsBySlug("electronics")).thenReturn(true);

        assertThat(adapter.existsBySlug("electronics")).isTrue();
    }

    @Test
    void findDescendantIdsDelegatesToJpa() {
        when(jpa.findDescendantIds(CATEGORY_ID)).thenReturn(List.of("child-1", "child-2"));

        assertThat(adapter.findDescendantIds(CATEGORY_ID)).containsExactly("child-1", "child-2");
    }

    @Test
    void findActiveRootsMapsResults() {
        CategoryEntity entity = new CategoryEntity();
        Category domain = Category.create(CATEGORY_ID, null, "A", "a");
        when(jpa.findActiveRoots()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findActiveRoots()).containsExactly(domain);
    }

    @Test
    void findActiveChildrenMapsResults() {
        CategoryEntity entity = new CategoryEntity();
        Category domain = Category.create(CATEGORY_ID, "parent", "A", "a");
        when(jpa.findActiveByParentId("parent")).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findActiveChildren("parent")).containsExactly(domain);
    }

    @Test
    void findAllActiveMapsResults() {
        CategoryEntity entity = new CategoryEntity();
        Category domain = Category.create(CATEGORY_ID, null, "A", "a");
        when(jpa.findAllActive()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findAllActive()).containsExactly(domain);
    }
}
