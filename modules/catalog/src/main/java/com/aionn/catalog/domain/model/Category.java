package com.aionn.catalog.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.catalog.domain.event.CategoryEvents;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class Category extends AggregateRoot {

    private final String categoryId;
    private String parentId;
    private String name;
    private String slug;
    private String iconUrl;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private final List<Translation> translations = new ArrayList<>();

    public record Translation(String locale, String name) {
    }

    public List<Translation> translations() {
        return Collections.unmodifiableList(translations);
    }

    public Category(
            String categoryId,
            String parentId,
            String name,
            String slug,
            String iconUrl,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            List<Translation> translations) {
        this.categoryId = categoryId;
        this.parentId = parentId;
        this.name = name;
        this.slug = slug;
        this.iconUrl = iconUrl;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        if (translations != null) {
            this.translations.addAll(translations);
        }
    }

    public static Category create(String categoryId, String parentId, String name, String slug) {
        return create(categoryId, parentId, name, slug, Clock.systemUTC());
    }

    public static Category create(String categoryId, String parentId, String name, String slug, Clock clock) {
        if (name == null || name.isBlank()) {
            throw new CatalogException(CatalogErrorCode.INVALID_ARGUMENT, "name must not be blank");
        }
        if (slug == null || slug.isBlank()) {
            throw new CatalogException(CatalogErrorCode.INVALID_ARGUMENT, "slug must not be blank");
        }
        Instant now = clock.instant();
        String trimmedName = name.trim();
        String trimmedSlug = slug.trim();
        Category category = new Category(categoryId, parentId, trimmedName, trimmedSlug, null, true, now, now, null,
                List.of());
        category.registerEvent(new CategoryEvents.CategoryCreated(categoryId, parentId, trimmedName, trimmedSlug, now));
        return category;
    }

    public void update(String name, String iconUrl, Boolean active) {
        update(name, iconUrl, active, Clock.systemUTC());
    }

    public void update(String name, String iconUrl, Boolean active, Clock clock) {
        Guard.require(deletedAt == null,
                () -> new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND, "Cannot update deleted category"));
        boolean changed = false;
        if (name != null) {
            Guard.require(!name.isBlank(),
                    () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT, "name must not be blank"));
            String trimmed = name.trim();
            if (!trimmed.equals(this.name)) {
                this.name = trimmed;
                changed = true;
            }
        }
        if (iconUrl != null && !iconUrl.equals(this.iconUrl)) {
            this.iconUrl = iconUrl;
            changed = true;
        }
        if (active != null && active != this.active) {
            this.active = active;
            changed = true;
        }
        if (!changed) {
            return;
        }
        this.updatedAt = clock.instant();
        registerEvent(new CategoryEvents.CategoryUpdated(categoryId, this.name, this.iconUrl, this.active, updatedAt));
    }

    public void moveTo(String newParentId) {
        moveTo(newParentId, Clock.systemUTC());
    }

    public void moveTo(String newParentId, Clock clock) {
        Guard.require(deletedAt == null,
                () -> new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND, "Cannot move deleted category"));
        Guard.require(newParentId == null || !newParentId.equals(this.categoryId),
                () -> new CatalogException(CatalogErrorCode.CATEGORY_CYCLE, "Category cannot be its own parent"));
        String oldParent = this.parentId;
        this.parentId = newParentId;
        this.updatedAt = clock.instant();
        registerEvent(new CategoryEvents.CategoryMoved(categoryId, oldParent, newParentId, updatedAt));
    }

    public void markDeleted() {
        markDeleted(Clock.systemUTC());
    }

    public void markDeleted(Clock clock) {
        if (deletedAt != null) {
            return;
        }
        this.deletedAt = clock.instant();
        this.active = false;
        registerEvent(new CategoryEvents.CategoryDeleted(categoryId, deletedAt, deletedAt));
    }

    @Override
    protected String aggregateId() {
        return categoryId;
    }
}
