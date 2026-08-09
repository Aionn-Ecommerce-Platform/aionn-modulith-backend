package com.aionn.catalog.application.port.out.category;

import com.aionn.catalog.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryPersistencePort {

    Category save(Category category);

    Optional<Category> findById(String categoryId);

    Optional<Category> lockById(String categoryId);

    List<Category> lockMutationSet(String categoryId, List<String> additionalCategoryIds);

    boolean existsByParentAndName(String parentId, String name);

    boolean existsBySlug(String slug);

    List<String> findDescendantIds(String categoryId);

    List<Category> findActiveRoots();

    List<Category> findActiveChildren(String parentId);

    List<Category> findAllActive();

    boolean hasProducts(String categoryId);
}
