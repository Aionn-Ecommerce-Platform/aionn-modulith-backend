package com.aionn.catalog.application.service;

import java.time.ZoneOffset;

import com.aionn.catalog.application.dto.attribute.command.ConfigureFilterableCommand;
import com.aionn.catalog.application.dto.attribute.command.CreateAttributeTemplateCommand;
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
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.aionn.sharedkernel.testing.fixture.TestClocks.FIXED;

@ExtendWith(MockitoExtension.class)
class AttributeTemplateServiceTest {

        private static final String TEMPLATE_ID = "01HZTPL0000000000000000001";
        private static final String CATEGORY_ID = "01HZCAT0000000000000000001";

        @Mock
        private AttributeTemplatePersistencePort attributeTemplatePersistencePort;
        @Mock
        private CategoryPersistencePort categoryPersistencePort;
        @Mock
        private EventPublisher eventPublisher;

        @Spy
        private Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

        @InjectMocks
        private AttributeTemplateService attributeTemplateService;

        @Test
        void createPersistsTemplateAndPublishesEvent() {
                Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics", FIXED);
                when(categoryPersistencePort.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                when(attributeTemplatePersistencePort.findByCategoryId(CATEGORY_ID)).thenReturn(Optional.empty());
                when(attributeTemplatePersistencePort.save(any(AttributeTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                AttributeTemplate result = attributeTemplateService.create(
                                new CreateAttributeTemplateCommand(CATEGORY_ID, List.of("color", "size")));

                assertThat(result).isNotNull();
                assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
                verify(attributeTemplatePersistencePort).save(any(AttributeTemplate.class));
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void createThrowsWhenCategoryMissing() {
                when(categoryPersistencePort.findById("missing")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> attributeTemplateService.create(
                                new CreateAttributeTemplateCommand("missing", List.of("color"))))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_NOT_FOUND.getCode());

                verify(attributeTemplatePersistencePort, never()).save(any());
        }

        @Test
        void createThrowsWhenTemplateAlreadyExistsForCategory() {
                Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics", FIXED);
                when(categoryPersistencePort.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                AttributeTemplate existing = AttributeTemplate.create(
                                TEMPLATE_ID, CATEGORY_ID, List.of("color"), FIXED);
                when(attributeTemplatePersistencePort.findByCategoryId(CATEGORY_ID))
                                .thenReturn(Optional.of(existing));

                assertThatThrownBy(() -> attributeTemplateService.create(
                                new CreateAttributeTemplateCommand(CATEGORY_ID, List.of("color"))))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());

                verify(attributeTemplatePersistencePort, never()).save(any());
        }

        @Test
        void configureFilterableUpdatesExistingAttribute() {
                AttributeTemplate template = AttributeTemplate.create(TEMPLATE_ID, CATEGORY_ID, List.of("color"), FIXED);
                template.pullEvents();
                when(attributeTemplatePersistencePort.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(attributeTemplatePersistencePort.save(template)).thenReturn(template);

                attributeTemplateService.configureFilterable(
                                new ConfigureFilterableCommand(TEMPLATE_ID, "color", false));

                assertThat(template.snapshot().get("color").filterable()).isFalse();
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void configureFilterableThrowsWhenTemplateMissing() {
                when(attributeTemplatePersistencePort.findById("nope")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> attributeTemplateService.configureFilterable(
                                new ConfigureFilterableCommand("nope", "color", true)))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.ATTRIBUTE_TEMPLATE_NOT_FOUND.getCode());
        }

        @Test
        void getReturnsResultWhenTemplateFound() {
                AttributeTemplate template = AttributeTemplate.create(TEMPLATE_ID, CATEGORY_ID, List.of("color"), FIXED);
                when(attributeTemplatePersistencePort.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

                AttributeTemplate result = attributeTemplateService.get(TEMPLATE_ID);

                assertThat(result.getTemplateId()).isEqualTo(TEMPLATE_ID);
                assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
        }

        @Test
        void getThrowsWhenTemplateMissing() {
                when(attributeTemplatePersistencePort.findById("nope")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> attributeTemplateService.get("nope"))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.ATTRIBUTE_TEMPLATE_NOT_FOUND.getCode());
        }

        @Test
        void getByCategoryReturnsResultWhenTemplateFound() {
                AttributeTemplate template = AttributeTemplate.create(TEMPLATE_ID, CATEGORY_ID, List.of("color"), FIXED);
                when(attributeTemplatePersistencePort.findByCategoryId(CATEGORY_ID)).thenReturn(Optional.of(template));

                AttributeTemplate result = attributeTemplateService.getByCategory(CATEGORY_ID);

                assertThat(result.getTemplateId()).isEqualTo(TEMPLATE_ID);
                assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
        }

        @Test
        void getByCategoryThrowsWhenNotFound() {
                when(attributeTemplatePersistencePort.findByCategoryId("no-category")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> attributeTemplateService.getByCategory("no-category"))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.ATTRIBUTE_TEMPLATE_NOT_FOUND.getCode());
        }
}
