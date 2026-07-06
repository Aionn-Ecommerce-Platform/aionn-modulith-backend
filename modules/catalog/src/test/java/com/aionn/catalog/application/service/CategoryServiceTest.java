package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.category.command.CreateCategoryCommand;
import com.aionn.catalog.application.dto.category.command.MoveCategoryCommand;
import com.aionn.catalog.application.dto.category.command.UpdateCategoryCommand;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.dto.category.result.CategoryTreeNode;
import com.aionn.catalog.application.mapper.CategoryResultMapper;
import com.aionn.catalog.application.port.out.category.CategoryPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Category;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

        private static final String CATEGORY_ID = "01HZCAT0000000000000000001";

        @Mock
        private CategoryPersistencePort categoryRepository;
        @Mock
        private CategoryResultMapper categoryResultMapper;
        @Mock
        private EventPublisher eventPublisher;

        @InjectMocks
        private CategoryService categoryService;

        private CategoryResult sampleResult;

        @BeforeEach
        void setUp() {
                sampleResult = new CategoryResult(
                                CATEGORY_ID, null, "Electronics", "electronics",
                                null, true, Instant.now(), Instant.now());
        }

        @Test
        void createPersistsCategoryWhenNameAndSlugAreFree() {
                when(categoryRepository.existsByParentAndName(null, "Electronics")).thenReturn(false);
                when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
                when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
                when(categoryResultMapper.toResult(any(Category.class))).thenReturn(sampleResult);

                CategoryResult result = categoryService.create(
                                new CreateCategoryCommand(null, "Electronics", "electronics"));

                assertThat(result).isEqualTo(sampleResult);
                ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
                verify(categoryRepository).save(captor.capture());
                assertThat(captor.getValue().getName()).isEqualTo("Electronics");
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void createTrimsNameBeforeUniquenessCheck() {
                when(categoryRepository.existsByParentAndName(null, "Electronics")).thenReturn(false);
                when(categoryRepository.existsBySlug(anyString())).thenReturn(false);
                when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
                when(categoryResultMapper.toResult(any(Category.class))).thenReturn(sampleResult);

                categoryService.create(new CreateCategoryCommand(null, "  Electronics  ", null));

                ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
                verify(categoryRepository).save(captor.capture());
                assertThat(captor.getValue().getName()).isEqualTo("Electronics");
        }

        @Test
        void createGeneratesSlugFromNameWhenSlugBlank() {
                when(categoryRepository.existsByParentAndName(null, "Home Appliances")).thenReturn(false);
                when(categoryRepository.existsBySlug(anyString())).thenReturn(false);
                when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
                when(categoryResultMapper.toResult(any(Category.class))).thenReturn(sampleResult);

                categoryService.create(new CreateCategoryCommand(null, "Home Appliances", null));

                ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
                verify(categoryRepository).save(captor.capture());
                assertThat(captor.getValue().getSlug()).isNotBlank();
        }

        @Test
        void createThrowsWhenParentNotFound() {
                when(categoryRepository.findById("missing")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> categoryService.create(
                                new CreateCategoryCommand("missing", "Electronics", "electronics")))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_NOT_FOUND.getCode());

                verify(categoryRepository, never()).save(any());
        }

        @Test
        void createThrowsOnNameConflict() {
                when(categoryRepository.existsByParentAndName(null, "Electronics")).thenReturn(true);

                assertThatThrownBy(() -> categoryService.create(
                                new CreateCategoryCommand(null, "Electronics", "electronics")))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_NAME_CONFLICT.getCode());

                verify(categoryRepository, never()).save(any());
        }

        @Test
        void createThrowsOnSlugConflict() {
                when(categoryRepository.existsByParentAndName(null, "Electronics")).thenReturn(false);
                when(categoryRepository.existsBySlug("electronics")).thenReturn(true);

                assertThatThrownBy(() -> categoryService.create(
                                new CreateCategoryCommand(null, "Electronics", "electronics")))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_SLUG_CONFLICT.getCode());
        }

        @Test
        void updateAppliesChangesAndPublishesEvent() {
                Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
                category.pullEvents();
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
                when(categoryResultMapper.toResult(any(Category.class))).thenReturn(sampleResult);

                categoryService.update(
                                new UpdateCategoryCommand(CATEGORY_ID, "Consumer Electronics", "https://icon", false));

                assertThat(category.getName()).isEqualTo("Consumer Electronics");
                assertThat(category.getIconUrl()).isEqualTo("https://icon");
                assertThat(category.isActive()).isFalse();
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void updateTrimsNameBeforeUniquenessCheck() {
                Category category = Category.create(CATEGORY_ID, null, "Old", "old");
                category.pullEvents();
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                when(categoryRepository.existsByParentAndName(null, "Zenith")).thenReturn(true);

                assertThatThrownBy(() -> categoryService.update(
                                new UpdateCategoryCommand(CATEGORY_ID, "  Zenith  ", null, null)))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_NAME_CONFLICT.getCode());
        }

        @Test
        void updateRejectsWhenNameCollides() {
                Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
                category.pullEvents();
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                when(categoryRepository.existsByParentAndName(null, "Fashion")).thenReturn(true);

                assertThatThrownBy(() -> categoryService.update(
                                new UpdateCategoryCommand(CATEGORY_ID, "Fashion", null, null)))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_NAME_CONFLICT.getCode());

                verify(categoryRepository, never()).save(any());
        }

        @Test
        void moveRejectsCycleWhenTargetParentIsDescendant() {
                Category root = Category.create("root", null, "A", "a");
                root.pullEvents();
                when(categoryRepository.findById("root")).thenReturn(Optional.of(root));
                when(categoryRepository.findById("child"))
                                .thenReturn(Optional.of(Category.create("child", "root", "B", "b")));
                when(categoryRepository.findDescendantIds("root")).thenReturn(List.of("child"));

                assertThatThrownBy(() -> categoryService.move(new MoveCategoryCommand("root", "child")))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_CYCLE.getCode());
        }

        @Test
        void moveRejectsSelfParent() {
                Category category = Category.create(CATEGORY_ID, null, "A", "a");
                category.pullEvents();
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

                assertThatThrownBy(() -> categoryService.move(new MoveCategoryCommand(CATEGORY_ID, CATEGORY_ID)))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_CYCLE.getCode());
        }

        @Test
        void moveRejectsWhenNewParentDoesNotExist() {
                Category category = Category.create(CATEGORY_ID, null, "A", "a");
                category.pullEvents();
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                when(categoryRepository.findById("missing")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> categoryService.move(new MoveCategoryCommand(CATEGORY_ID, "missing")))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_NOT_FOUND.getCode());
        }

        @Test
        void moveRejectsWhenSiblingWithSameNameExists() {
                Category category = Category.create(CATEGORY_ID, null, "Duplicate", "duplicate");
                category.pullEvents();
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                when(categoryRepository.findById("new-parent"))
                                .thenReturn(Optional.of(Category.create("new-parent", null, "P", "p")));
                when(categoryRepository.findDescendantIds(CATEGORY_ID)).thenReturn(List.of());
                when(categoryRepository.existsByParentAndName("new-parent", "Duplicate")).thenReturn(true);

                assertThatThrownBy(() -> categoryService.move(new MoveCategoryCommand(CATEGORY_ID, "new-parent")))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_NAME_CONFLICT.getCode());
        }

        @Test
        void moveAppliesReparent() {
                Category category = Category.create(CATEGORY_ID, null, "A", "a");
                category.pullEvents();
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                when(categoryRepository.findById("new-parent"))
                                .thenReturn(Optional.of(Category.create("new-parent", null, "P", "p")));
                when(categoryRepository.findDescendantIds(CATEGORY_ID)).thenReturn(List.of());
                when(categoryRepository.existsByParentAndName("new-parent", "A")).thenReturn(false);
                when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
                when(categoryResultMapper.toResult(any(Category.class))).thenReturn(sampleResult);

                categoryService.move(new MoveCategoryCommand(CATEGORY_ID, "new-parent"));

                assertThat(category.getParentId()).isEqualTo("new-parent");
        }

        @Test
        void deleteSoftDeletesAndPublishesEvent() {
                Category category = Category.create(CATEGORY_ID, null, "A", "a");
                category.pullEvents();
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

                categoryService.delete(CATEGORY_ID);

                assertThat(category.getDeletedAt()).isNotNull();
                assertThat(category.isActive()).isFalse();
                verify(categoryRepository).save(category);
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void deleteThrowsWhenNotFound() {
                when(categoryRepository.findById("missing")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> categoryService.delete("missing"))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.CATEGORY_NOT_FOUND.getCode());
        }

        @Test
        void getReturnsResultWhenFound() {
                Category category = Category.create(CATEGORY_ID, null, "A", "a");
                when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
                when(categoryResultMapper.toResult(category)).thenReturn(sampleResult);

                assertThat(categoryService.get(CATEGORY_ID)).isEqualTo(sampleResult);
        }

        @Test
        void listRootsMapsResults() {
                Category category = Category.create(CATEGORY_ID, null, "A", "a");
                when(categoryRepository.findActiveRoots()).thenReturn(List.of(category));
                when(categoryResultMapper.toResult(category)).thenReturn(sampleResult);

                List<CategoryResult> roots = categoryService.listRoots();

                assertThat(roots).containsExactly(sampleResult);
        }

        @Test
        void listChildrenMapsResults() {
                Category category = Category.create(CATEGORY_ID, "parent", "A", "a");
                when(categoryRepository.findActiveChildren("parent")).thenReturn(List.of(category));
                when(categoryResultMapper.toResult(category)).thenReturn(sampleResult);

                assertThat(categoryService.listChildren("parent")).containsExactly(sampleResult);
        }

        @Test
        void getTreeBuildsHierarchyFromActiveCategories() {
                Category root = Category.create("root", null, "Root", "root");
                Category child = Category.create("child", "root", "Child", "child");
                when(categoryRepository.findAllActive()).thenReturn(List.of(root, child));

                CategoryResult rootResult = new CategoryResult(
                                "root", null, "Root", "root", null, true, Instant.now(), Instant.now());
                CategoryResult childResult = new CategoryResult(
                                "child", "root", "Child", "child", null, true, Instant.now(), Instant.now());
                when(categoryResultMapper.toResult(root)).thenReturn(rootResult);
                when(categoryResultMapper.toResult(child)).thenReturn(childResult);

                List<CategoryTreeNode> tree = categoryService.getTree();

                assertThat(tree).hasSize(1);
                assertThat(tree.get(0).category().categoryId()).isEqualTo("root");
                assertThat(tree.get(0).children()).hasSize(1);
                assertThat(tree.get(0).children().get(0).category().categoryId()).isEqualTo("child");
        }

        @Test
        void getTreeReturnsEmptyWhenNoActiveCategories() {
                when(categoryRepository.findAllActive()).thenReturn(List.of());

                assertThat(categoryService.getTree()).isEmpty();
        }
}
