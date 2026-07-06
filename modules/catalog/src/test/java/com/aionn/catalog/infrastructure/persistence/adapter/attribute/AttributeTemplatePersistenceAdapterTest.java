package com.aionn.catalog.infrastructure.persistence.adapter.attribute;

import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.catalog.infrastructure.persistence.entity.AttributeTemplateEntity;
import com.aionn.catalog.infrastructure.persistence.mapper.AttributeTemplateDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.attribute.AttributeTemplateRepository;
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
class AttributeTemplatePersistenceAdapterTest {

    private static final String TEMPLATE_ID = "01HZTPL0000000000000000001";
    private static final String CATEGORY_ID = "01HZCAT0000000000000000001";

    @Mock
    private AttributeTemplateRepository jpa;
    @Mock
    private AttributeTemplateDomainMapper mapper;

    @InjectMocks
    private AttributeTemplatePersistenceAdapter adapter;

    @Test
    void saveMapsThroughEntityAndBack() {
        AttributeTemplate domain = AttributeTemplate.create(TEMPLATE_ID, CATEGORY_ID, List.of("color"));
        AttributeTemplateEntity entity = new AttributeTemplateEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.save(domain)).isSameAs(domain);
    }

    @Test
    void findByIdReturnsMappedDomainWhenPresent() {
        AttributeTemplateEntity entity = new AttributeTemplateEntity();
        AttributeTemplate domain = AttributeTemplate.create(TEMPLATE_ID, CATEGORY_ID, List.of("color"));
        when(jpa.findById(TEMPLATE_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(TEMPLATE_ID)).contains(domain);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(jpa.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void findByCategoryIdReturnsMappedDomainWhenPresent() {
        AttributeTemplateEntity entity = new AttributeTemplateEntity();
        AttributeTemplate domain = AttributeTemplate.create(TEMPLATE_ID, CATEGORY_ID, List.of("color"));
        when(jpa.findByCategoryId(CATEGORY_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByCategoryId(CATEGORY_ID)).contains(domain);
    }

    @Test
    void findByCategoryIdsReturnsEmptyForNullInput() {
        assertThat(adapter.findByCategoryIds(null)).isEmpty();
    }

    @Test
    void findByCategoryIdsReturnsEmptyForEmptyInput() {
        assertThat(adapter.findByCategoryIds(List.of())).isEmpty();
    }

    @Test
    void findByCategoryIdsMapsResults() {
        AttributeTemplateEntity entity = new AttributeTemplateEntity();
        AttributeTemplate domain = AttributeTemplate.create(TEMPLATE_ID, CATEGORY_ID, List.of("color"));
        when(jpa.findByCategoryIdIn(List.of(CATEGORY_ID))).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByCategoryIds(List.of(CATEGORY_ID))).containsExactly(domain);
    }
}
