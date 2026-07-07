package com.aionn.catalog.infrastructure.persistence.adapter.brand;

import com.aionn.catalog.domain.model.Brand;
import com.aionn.catalog.infrastructure.persistence.entity.BrandEntity;
import com.aionn.catalog.infrastructure.persistence.mapper.BrandDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.brand.BrandRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandPersistenceAdapterTest {

    private static final String BRAND_ID = "01HZBRD0000000000000000001";

    @Mock
    private BrandRepository jpa;
    @Mock
    private BrandDomainMapper mapper;

    @InjectMocks
    private BrandPersistenceAdapter adapter;

    @Test
    void saveMapsThroughEntityAndBack() {
        Brand domain = Brand.create(BRAND_ID, "Acme", null, null);
        BrandEntity entity = new BrandEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        Brand saved = adapter.save(domain);

        assertThat(saved).isSameAs(domain);
    }

    @Test
    void findByIdReturnsMappedDomainWhenPresent() {
        BrandEntity entity = new BrandEntity();
        Brand domain = Brand.create(BRAND_ID, "Acme", null, null);
        when(jpa.findById(BRAND_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(BRAND_ID)).contains(domain);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(jpa.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void existsByNameDelegatesToJpaIgnoreCase() {
        when(jpa.existsByNameIgnoreCase("Acme")).thenReturn(true);

        assertThat(adapter.existsByName("Acme")).isTrue();
    }

    @Test
    void listReturnsMappedResult() {
        BrandEntity entity = new BrandEntity();
        Brand domain = Brand.create(BRAND_ID, "Acme", null, null);
        when(jpa.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Brand> brands = adapter.list(OffsetPagination.of(0, 10));

        assertThat(brands).containsExactly(domain);
    }

    @Test
    void listAppliesStableSortByCreatedAtThenBrandId() {
        BrandEntity entity = new BrandEntity();
        when(jpa.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(Brand.create(BRAND_ID, "Acme", null, null));

        adapter.list(OffsetPagination.of(0, 10));

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(jpa).findAll(captor.capture());
        Sort sort = captor.getValue().getSort();
        assertThat(sort.getOrderFor("createdAt")).isNotNull();
        assertThat(sort.getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(sort.getOrderFor("brandId")).isNotNull();
        assertThat(sort.getOrderFor("brandId").isAscending()).isTrue();
    }

    @Test
    void countDelegatesToJpa() {
        when(jpa.count()).thenReturn(7L);

        assertThat(adapter.count()).isEqualTo(7L);
    }
}
