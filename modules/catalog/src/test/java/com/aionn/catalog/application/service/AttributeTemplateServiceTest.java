package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.attribute.command.ConfigureFilterableCommand;
import com.aionn.catalog.application.dto.attribute.command.CreateAttributeTemplateCommand;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.application.mapper.AttributeTemplateResultMapper;
import com.aionn.catalog.application.port.out.attribute.AttributeTemplatePersistencePort;
import com.aionn.catalog.application.port.out.category.CategoryPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.AttributeTemplate;
import com.aionn.catalog.domain.model.Category;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttributeTemplateServiceTest {

        private static final String TEMPLATE_ID = "01HZTPL0000000000000000001";
        private static final String CATEGORY_ID = "01HZCAT0000000000000000001";

        @Mock
        private AttributeTemplatePersistencePort attributeTemplateRepository;
        @Mock
        private CategoryPersistencePort categoryRepository;
        @Mock
        private AttributeTemplateResultMapper attributeTemplateResultMapper;
        @Mock
        private EventPublisher eventPublisher;

        @InjectMocks
        private AttributeTemplateService attributeTemplateService;

        private AttributeTemplateResult sampleResult() {
                return new AttributeTemplateResult(
                                TEMPLATE_ID, CATEGORY_ID,
                                Map.of("color", true, "size", true),
                                Instant.now(), Instant.now());
        }

        @Test
        void createPersistsTemplateAndPublishesEvent() {
                Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                when(attributeTemplateRepository.findByCategoryId(CATEGORY_ID)).thenReturn(Optional.empty());
                when(attributeTemplateRepository.save(any(AttributeTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(attributeTemplateResultMapper.toResult(any(AttributeTemplate.class))).thenReturn(sampleResult());

                AttributeTemplateResult result = attributeTemplateService.create(
                                new CreateAttributeTemplateCommand(CATEGORY_ID, List.of("color", "size")));

                assertThat(result).isNotNull();
                verify(attributeTemplateRepository).save(any(AttributeTemplate.class));
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void createThrowsWhenCategoryMissing() {
                when(categoryRepository.findById("missing")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> attributeTemplateService.create(
                                new CreateAttributeTemplateCommand("missing", List.of("color"))))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_NOT_FOUND.getCode());

                verify(attributeTemplateRepository, never()).save(any());
        }

        @Test
        void createThrowsWhenTemplateAlreadyExistsForCategory() {
                Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                AttributeTemplate existing = AttributeTemplate.create(
                                TEMPLATE_ID, CATEGORY_ID, List.of("color"));
                when(attributeTemplateRepository.findByCategoryId(CATEGORY_ID))
                                .thenReturn(Optional.of(existing));

                assertThatThrownBy(() -> attributeTemplateService.create(
                                new CreateAttributeTemplateCommand(CATEGORY_ID, List.of("color"))))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());

                verify(attributeTemplateRepository, never()).save(any());
        }

        @Test
        void configureFilterableUpdatesExistingAttribute() {
                AttributeTemplate template = AttributeTemplate.create(TEMPLATE_ID, CATEGORY_ID, List.of("color"));
                template.pullEvents();
                when(attributeTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(attributeTemplateRepository.save(template)).thenReturn(template);
                when(attributeTemplateResultMapper.toResult(template)).thenReturn(sampleResult());

                attributeTemplateService.configureFilterable(
                                new ConfigureFilterableCommand(TEMPLATE_ID, "color", false));

                assertThat(template.snapshot().get("color").filterable()).isFalse();
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void configureFilterableThrowsWhenTemplateMissing() {
                when(attributeTemplateRepository.findById("nope")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> attributeTemplateService.configureFilterable(
                                new ConfigureFilterableCommand("nope", "color", true)))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.ATTRIBUTE_TEMPLATE_NOT_FOUND.getCode());
        }

        @Test
        void getReturnsResultWhenTemplateFound() {
                AttributeTemplate template = AttributeTemplate.create(TEMPLATE_ID, CATEGORY_ID, List.of("color"));
                AttributeTemplateResult result = sampleResult();
                when(attributeTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(attributeTemplateResultMapper.toResult(template)).thenReturn(result);

                assertThat(attributeTemplateService.get(TEMPLATE_ID)).isEqualTo(result);
        }

        @Test
        void getThrowsWhenTemplateMissing() {
                when(attributeTemplateRepository.findById("nope")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> attributeTemplateService.get("nope"))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.ATTRIBUTE_TEMPLATE_NOT_FOUND.getCode());
        }

        @Test
        void getByCategoryReturnsResultWhenTemplateFound() {
                AttributeTemplate template = AttributeTemplate.create(TEMPLATE_ID, CATEGORY_ID, List.of("color"));
                AttributeTemplateResult result = sampleResult();
                when(attributeTemplateRepository.findByCategoryId(CATEGORY_ID)).thenReturn(Optional.of(template));
                when(attributeTemplateResultMapper.toResult(template)).thenReturn(result);

                assertThat(attributeTemplateService.getByCategory(CATEGORY_ID)).isEqualTo(result);
        }

        @Test
        void getByCategoryThrowsWhenNotFound() {
                when(attributeTemplateRepository.findByCategoryId("no-category")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> attributeTemplateService.getByCategory("no-category"))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.ATTRIBUTE_TEMPLATE_NOT_FOUND.getCode());
        }
}
