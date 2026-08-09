package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.category.command.CreateCategoryCommand;
import com.aionn.catalog.application.dto.category.command.MoveCategoryCommand;
import com.aionn.catalog.application.dto.category.command.UpdateCategoryCommand;
import com.aionn.catalog.application.port.out.category.CategoryPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Category;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import com.aionn.sharedkernel.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryPersistencePort categoryRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public Category create(CreateCategoryCommand command) {
        String normalizedName = command.name() != null ? command.name().trim() : null;
        if (command.parentId() != null) {
            ensureUsableParent(lockRequired(command.parentId()));
        }
        if (categoryRepository.existsByParentAndName(command.parentId(), normalizedName)) {
            throw new CatalogException(CatalogErrorCode.CATEGORY_NAME_CONFLICT);
        }
        String slug = command.slug() != null && !command.slug().isBlank()
                ? command.slug().trim()
                : SlugUtils.slugify(normalizedName);
        if (categoryRepository.existsBySlug(slug)) {
            throw new CatalogException(CatalogErrorCode.CATEGORY_SLUG_CONFLICT);
        }
        Category category = Category.create(IdGenerator.ulid(), command.parentId(), normalizedName, slug, clock);
        Category saved = categoryRepository.save(category);
        eventPublisher.publish(category.pullEvents());
        return saved;
    }

    public Category update(UpdateCategoryCommand command) {
        Map<String, Category> locked = lockMutationSet(command.categoryId(), List.of());
        Category category = requiredLocked(locked, command.categoryId());
        if (Boolean.TRUE.equals(command.active()) && category.getParentId() != null) {
            ensureUsableParent(locked.get(category.getParentId()));
        }
        String normalizedName = command.name() != null ? command.name().trim() : null;
        if (normalizedName != null
                && !normalizedName.equalsIgnoreCase(category.getName())
                && categoryRepository.existsByParentAndName(category.getParentId(), normalizedName)) {
            throw new CatalogException(CatalogErrorCode.CATEGORY_NAME_CONFLICT);
        }
        category.update(normalizedName, command.iconUrl(), command.active(), clock);
        Category saved = categoryRepository.save(category);
        eventPublisher.publish(category.pullEvents());
        return saved;
    }

    public Category move(MoveCategoryCommand command) {
        Map<String, Category> locked = lockMutationSet(
                command.categoryId(), java.util.Collections.singletonList(command.newParentId()));
        Category category = requiredLocked(locked, command.categoryId());
        if (command.newParentId() != null) {
            if (command.newParentId().equals(category.getCategoryId())) {
                throw new CatalogException(CatalogErrorCode.CATEGORY_CYCLE);
            }
            ensureUsableParent(locked.get(command.newParentId()));
            if (categoryRepository.findDescendantIds(category.getCategoryId())
                    .contains(command.newParentId())) {
                throw new CatalogException(CatalogErrorCode.CATEGORY_CYCLE);
            }
            if (categoryRepository.existsByParentAndName(command.newParentId(), category.getName())) {
                throw new CatalogException(CatalogErrorCode.CATEGORY_NAME_CONFLICT);
            }
        }
        category.moveTo(command.newParentId(), clock);
        Category saved = categoryRepository.save(category);
        eventPublisher.publish(category.pullEvents());
        return saved;
    }

    public void delete(String categoryId) {
        Category category = lockRequired(categoryId);
        if (categoryRepository.hasProducts(categoryId)) {
            throw new CatalogException(CatalogErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        if (!categoryRepository.findActiveChildren(categoryId).isEmpty()) {
            throw new CatalogException(CatalogErrorCode.CATEGORY_HAS_CHILDREN);
        }
        category.markDeleted(clock);
        categoryRepository.save(category);
        eventPublisher.publish(category.pullEvents());
    }

    @Transactional(readOnly = true)
    public Category get(String categoryId) {
        return required(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Category> listRoots() {
        return categoryRepository.findActiveRoots();
    }

    @Transactional(readOnly = true)
    public List<Category> listChildren(String parentId) {
        return categoryRepository.findActiveChildren(parentId);
    }

    @Transactional(readOnly = true)
    public List<Category> getTree() {
        return categoryRepository.findAllActive();
    }

    private Category required(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND));
    }

    private Category lockRequired(String categoryId) {
        return categoryRepository.lockById(categoryId)
                .or(() -> categoryRepository.findById(categoryId))
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND));
    }

    private Map<String, Category> lockInStableOrder(String... categoryIds) {
        Map<String, Category> locked = new LinkedHashMap<>();
        java.util.Arrays.stream(categoryIds)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(id -> locked.put(id, lockRequired(id)));
        return locked;
    }

    private Map<String, Category> lockMutationSet(String categoryId, List<String> additionalIds) {
        Map<String, Category> locked = new LinkedHashMap<>();
        categoryRepository.lockMutationSet(categoryId, additionalIds).stream()
                .sorted(java.util.Comparator.comparing(Category::getCategoryId))
                .forEach(category -> locked.put(category.getCategoryId(), category));
        return locked;
    }

    private static Category requiredLocked(Map<String, Category> locked, String categoryId) {
        Category category = locked.get(categoryId);
        if (category == null) {
            throw new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private static void ensureUsableParent(Category parent) {
        if (parent == null || parent.getDeletedAt() != null) {
            throw new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND, "Parent category not found");
        }
    }
}
