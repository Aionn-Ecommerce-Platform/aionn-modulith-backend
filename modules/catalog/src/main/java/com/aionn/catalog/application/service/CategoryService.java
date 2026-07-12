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
import com.aionn.sharedkernel.util.IdGenerator;
import com.aionn.sharedkernel.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryPersistencePort categoryRepository;
    private final CategoryResultMapper categoryResultMapper;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public CategoryResult create(CreateCategoryCommand command) {
        String normalizedName = command.name() != null ? command.name().trim() : null;
        if (command.parentId() != null && categoryRepository.findById(command.parentId()).isEmpty()) {
            throw new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND, "Parent category not found");
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
        return categoryResultMapper.toResult(saved);
    }

    public CategoryResult update(UpdateCategoryCommand command) {
        Category category = required(command.categoryId());
        String normalizedName = command.name() != null ? command.name().trim() : null;
        if (normalizedName != null
                && !normalizedName.equalsIgnoreCase(category.getName())
                && categoryRepository.existsByParentAndName(category.getParentId(), normalizedName)) {
            throw new CatalogException(CatalogErrorCode.CATEGORY_NAME_CONFLICT);
        }
        category.update(normalizedName, command.iconUrl(), command.active(), clock);
        Category saved = categoryRepository.save(category);
        eventPublisher.publish(category.pullEvents());
        return categoryResultMapper.toResult(saved);
    }

    public CategoryResult move(MoveCategoryCommand command) {
        Category category = required(command.categoryId());
        if (command.newParentId() != null) {
            if (command.newParentId().equals(category.getCategoryId())) {
                throw new CatalogException(CatalogErrorCode.CATEGORY_CYCLE);
            }
            categoryRepository.findById(command.newParentId())
                    .orElseThrow(() -> new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND,
                            "Target parent does not exist"));
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
        return categoryResultMapper.toResult(saved);
    }

    public void delete(String categoryId) {
        Category category = required(categoryId);
        if (categoryRepository.hasProducts(categoryId)) {
            throw new CatalogException(CatalogErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        category.markDeleted(clock);
        categoryRepository.save(category);
        eventPublisher.publish(category.pullEvents());
    }

    @Transactional(readOnly = true)
    public CategoryResult get(String categoryId) {
        return categoryResultMapper.toResult(required(categoryId));
    }

    @Transactional(readOnly = true)
    public List<CategoryResult> listRoots() {
        return categoryRepository.findActiveRoots().stream()
                .map(categoryResultMapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResult> listChildren(String parentId) {
        return categoryRepository.findActiveChildren(parentId).stream()
                .map(categoryResultMapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeNode> getTree() {
        List<Category> allActive = categoryRepository.findAllActive();
        List<CategoryResult> results = allActive.stream()
                .map(categoryResultMapper::toResult)
                .toList();

        Map<String, List<CategoryResult>> byParent = results.stream()
                .filter(c -> c.parentId() != null)
                .collect(Collectors.groupingBy(CategoryResult::parentId));

        List<CategoryResult> roots = results.stream()
                .filter(c -> c.parentId() == null)
                .toList();

        return roots.stream()
                .map(root -> buildNode(root, byParent))
                .toList();
    }

    private CategoryTreeNode buildNode(CategoryResult category, Map<String, List<CategoryResult>> byParent) {
        List<CategoryResult> children = byParent.getOrDefault(category.categoryId(), List.of());
        List<CategoryTreeNode> childNodes = children.stream()
                .map(child -> buildNode(child, byParent))
                .toList();
        return new CategoryTreeNode(category, childNodes);
    }

    private Category required(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND));
    }
}
