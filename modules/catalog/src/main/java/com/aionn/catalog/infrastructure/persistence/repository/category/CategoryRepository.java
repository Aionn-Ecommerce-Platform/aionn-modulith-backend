package com.aionn.catalog.infrastructure.persistence.repository.category;

import com.aionn.catalog.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<CategoryEntity, String> {

        @Query("""
                        SELECT (count(c) > 0) FROM CategoryEntity c
                          WHERE LOWER(c.name) = LOWER(:name)
                            AND c.deletedAt IS NULL
                            AND ((:parentId IS NULL AND c.parentId IS NULL)
                                 OR c.parentId = :parentId)
                        """)
        boolean existsByParentIdAndNameIgnoreCase(String parentId, String name);

        @Query("""
                        SELECT (count(c) > 0) FROM CategoryEntity c
                          WHERE c.slug = :slug
                            AND c.deletedAt IS NULL
                        """)
        boolean existsBySlug(String slug);

        @EntityGraph(attributePaths = "translations")
        @Query("SELECT c FROM CategoryEntity c WHERE c.parentId IS NULL AND c.deletedAt IS NULL AND c.active = true")
        List<CategoryEntity> findActiveRoots();

        @EntityGraph(attributePaths = "translations")
        @Query("SELECT c FROM CategoryEntity c WHERE c.parentId = :parentId AND c.deletedAt IS NULL AND c.active = true")
        List<CategoryEntity> findActiveByParentId(String parentId);

        @EntityGraph(attributePaths = "translations")
        @Query("SELECT c FROM CategoryEntity c WHERE c.deletedAt IS NULL AND c.active = true")
        List<CategoryEntity> findAllActive();

        @Query(value = """
                        WITH RECURSIVE descendants AS (
                            SELECT category_id FROM categories WHERE parent_id = :categoryId
                            UNION ALL
                            SELECT c.category_id FROM categories c
                              JOIN descendants d ON c.parent_id = d.category_id
                        )
                        SELECT category_id FROM descendants
                        """, nativeQuery = true)
        List<String> findDescendantIds(String categoryId);
}
