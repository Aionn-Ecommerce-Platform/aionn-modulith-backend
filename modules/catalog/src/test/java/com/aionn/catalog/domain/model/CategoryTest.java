package com.aionn.catalog.domain.model;

import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    private static final String CATEGORY_ID = "01HZCAT0000000000000000001";

    @Test
    void createSetsActiveTrueAndEmitsCreatedEvent() {
        Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");

        assertThat(category.isActive()).isTrue();
        assertThat(category.getDeletedAt()).isNull();
        assertThat(category.pullEvents()).hasSize(1);
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Category.create(CATEGORY_ID, null, "", "x"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void createRejectsBlankSlug() {
        assertThatThrownBy(() -> Category.create(CATEGORY_ID, null, "Electronics", " "))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void createRejectsNullSlug() {
        assertThatThrownBy(() -> Category.create(CATEGORY_ID, null, "Electronics", null))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void createTrimsNameAndSlugConsistentlyOnStateAndEvent() {
        Category category = Category.create(CATEGORY_ID, null, "  Electronics  ", "  electronics  ");

        assertThat(category.getName()).isEqualTo("Electronics");
        assertThat(category.getSlug()).isEqualTo("electronics");
        var events = category.pullEvents();
        assertThat(events).hasSize(1);
        var created = (com.aionn.catalog.domain.event.CategoryEvents.CategoryCreated) events.get(0).payload();
        assertThat(created.name()).isEqualTo("Electronics");
        assertThat(created.slug()).isEqualTo("electronics");
    }

    @Test
    void updateAppliesAllFieldsWhenProvided() {
        Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
        category.pullEvents();

        category.update("Renamed", "https://icon", false);

        assertThat(category.getName()).isEqualTo("Renamed");
        assertThat(category.getIconUrl()).isEqualTo("https://icon");
        assertThat(category.isActive()).isFalse();
        assertThat(category.pullEvents()).hasSize(1);
    }

    @Test
    void updateEmitsNoEventWhenNothingChanged() {
        Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
        category.pullEvents();

        category.update(null, null, null);

        assertThat(category.getName()).isEqualTo("Electronics");
        assertThat(category.isActive()).isTrue();
        assertThat(category.pullEvents()).isEmpty();
    }

    @Test
    void updateEmitsNoEventWhenAllValuesEqualCurrent() {
        Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
        category.update("Renamed", "https://icon", false);
        category.pullEvents();

        category.update("Renamed", "https://icon", false);

        assertThat(category.pullEvents()).isEmpty();
    }

    @Test
    void updateRejectsBlankName() {
        Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
        category.pullEvents();

        assertThatThrownBy(() -> category.update("  ", null, null))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void moveToSelfThrowsCycle() {
        Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");

        assertThatThrownBy(() -> category.moveTo(CATEGORY_ID))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.CATEGORY_CYCLE.getCode());
    }

    @Test
    void moveToNewParentSucceeds() {
        Category category = Category.create(CATEGORY_ID, "old-parent", "A", "a");
        category.pullEvents();

        category.moveTo("new-parent");

        assertThat(category.getParentId()).isEqualTo("new-parent");
        assertThat(category.pullEvents()).hasSize(1);
    }

    @Test
    void moveToNullParentBecomesRoot() {
        Category category = Category.create(CATEGORY_ID, "old-parent", "A", "a");
        category.pullEvents();

        category.moveTo(null);

        assertThat(category.getParentId()).isNull();
    }

    @Test
    void markDeletedSetsTimestampAndDeactivates() {
        Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");

        category.markDeleted();

        assertThat(category.getDeletedAt()).isNotNull();
        assertThat(category.isActive()).isFalse();
    }

    @Test
    void markDeletedIsIdempotent() {
        Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
        category.markDeleted();
        category.pullEvents();

        category.markDeleted();

        assertThat(category.pullEvents()).isEmpty();
    }

    @Test
    void updateOnDeletedCategoryThrows() {
        Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
        category.markDeleted();

        assertThatThrownBy(() -> category.update("Renamed", null, true))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.CATEGORY_NOT_FOUND.getCode());
    }

    @Test
    void moveOnDeletedCategoryThrows() {
        Category category = Category.create(CATEGORY_ID, null, "Electronics", "electronics");
        category.markDeleted();

        assertThatThrownBy(() -> category.moveTo("new-parent"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.CATEGORY_NOT_FOUND.getCode());
    }

    @Test
    void translationsAccessorReturnsImmutableSnapshot() {
        Category.Translation vi = new Category.Translation("vi", "Điện tử");
        Category category = new Category(
                CATEGORY_ID, null, "Electronics", "electronics", null,
                true, Instant.now(), Instant.now(), null, List.of(vi));

        List<Category.Translation> translations = category.translations();

        assertThat(translations).containsExactly(vi);
        assertThat(translations.get(0).locale()).isEqualTo("vi");
        assertThat(translations.get(0).name()).isEqualTo("Điện tử");
    }
}
