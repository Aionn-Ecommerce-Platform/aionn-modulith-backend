package com.aionn.catalog.application.service;

import com.aionn.catalog.application.port.out.merchant.CollectionPersistencePort;
import com.aionn.catalog.application.port.out.merchant.CollectionPersistencePort.CollectionView;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

    @Mock
    private CollectionPersistencePort repository;

    private CollectionService service;

    @BeforeEach
    void setUp() {
        service = new CollectionService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createNormalizesTheInputAndStoresIt() {
        when(repository.save(any(CollectionView.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CollectionView created = service.create("  Summer-Sale ", "  Summer Sale  ", "  ", "",
                true, -3);

        assertThat(created.collectionId()).startsWith("col_");
        assertThat(created.slug()).isEqualTo("summer-sale");
        assertThat(created.name()).isEqualTo("Summer Sale");
        assertThat(created.description()).isNull();
        assertThat(created.imageUrl()).isNull();
        assertThat(created.displayOrder()).isZero();
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void createTranslatesADuplicateSlugIntoTheDomainError() {
        when(repository.save(any(CollectionView.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.create("summer", "Summer", null, null, true, 0))
                .isInstanceOfSatisfying(CatalogException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(CatalogErrorCode.COLLECTION_SLUG_CONFLICT.getCode()));
    }

    @Test
    void createRejectsInvalidSlugs() {
        assertThatThrownBy(() -> service.create(" ", "Name", null, null, true, 0))
                .isInstanceOf(CatalogException.class);
        assertThatThrownBy(() -> service.create("bad slug!", "Name", null, null, true, 0))
                .isInstanceOf(CatalogException.class);
        assertThatThrownBy(() -> service.create("a".repeat(101), "Name", null, null, true, 0))
                .isInstanceOf(CatalogException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsInvalidNames() {
        assertThatThrownBy(() -> service.create("slug", "  ", null, null, true, 0))
                .isInstanceOf(CatalogException.class);
        assertThatThrownBy(() -> service.create("slug", "n".repeat(256), null, null, true, 0))
                .isInstanceOf(CatalogException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateKeepsFieldsThatWereNotProvided() {
        CollectionView existing = existing();
        when(repository.findByIdOrSlug("col_1")).thenReturn(Optional.of(existing));
        when(repository.save(any(CollectionView.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CollectionView updated = service.update("col_1", null, null, "  new description ",
                "https://img", false, 7);

        assertThat(updated.slug()).isEqualTo(existing.slug());
        assertThat(updated.name()).isEqualTo(existing.name());
        assertThat(updated.description()).isEqualTo("new description");
        assertThat(updated.imageUrl()).isEqualTo("https://img");
        assertThat(updated.active()).isFalse();
        assertThat(updated.displayOrder()).isEqualTo(7);
        assertThat(updated.createdAt()).isEqualTo(existing.createdAt());
        assertThat(updated.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void updateNormalizesAProvidedSlugAndTranslatesConflicts() {
        when(repository.findByIdOrSlug("col_1")).thenReturn(Optional.of(existing()));
        ArgumentCaptor<CollectionView> captor = ArgumentCaptor.forClass(CollectionView.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update("col_1", " New-Slug ", "New name", null, null, true, 0);

        assertThat(captor.getValue().slug()).isEqualTo("new-slug");
        assertThat(captor.getValue().name()).isEqualTo("New name");
    }

    @Test
    void updateReportsAMissingCollection() {
        when(repository.findByIdOrSlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("missing", null, null, null, null, true, 0))
                .isInstanceOfSatisfying(CatalogException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(CatalogErrorCode.COLLECTION_NOT_FOUND.getCode()));
    }

    @Test
    void deleteRefusesCollectionsThatStillContainProducts() {
        when(repository.findByIdOrSlug("col_1")).thenReturn(Optional.of(existing()));
        when(repository.countProducts("col_1")).thenReturn(3L);

        assertThatThrownBy(() -> service.delete("col_1"))
                .isInstanceOfSatisfying(CatalogException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(CatalogErrorCode.COLLECTION_HAS_PRODUCTS.getCode()));
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteRemovesAnEmptyCollection() {
        when(repository.findByIdOrSlug("col_1")).thenReturn(Optional.of(existing()));
        when(repository.countProducts("col_1")).thenReturn(0L);

        service.delete("col_1");

        verify(repository).delete("col_1");
    }

    @Test
    void requireExistingSkipsEmptyInputAndRejectsUnknownIds() {
        service.requireExisting(null);
        service.requireExisting(List.of());
        verify(repository, never()).existsAll(any());

        when(repository.existsAll(List.of("a", "b"))).thenReturn(false);
        assertThatThrownBy(() -> service.requireExisting(List.of("a", "b")))
                .isInstanceOfSatisfying(CatalogException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(CatalogErrorCode.COLLECTION_NOT_FOUND.getCode()));
    }

    @Test
    void requireExistingAcceptsKnownIds() {
        when(repository.existsAll(List.of("a"))).thenReturn(true);

        service.requireExisting(List.of("a"));

        verify(repository).existsAll(List.of("a"));
    }

    @Test
    void listAndGetDelegateToTheRepository() {
        when(repository.findAll(true)).thenReturn(List.of(existing()));
        when(repository.findByIdOrSlug("missing")).thenReturn(Optional.empty());

        assertThat(service.list(true)).hasSize(1);
        assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOfSatisfying(CatalogException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(CatalogErrorCode.COLLECTION_NOT_FOUND.getCode()));
    }

    private static CollectionView existing() {
        return new CollectionView("col_1", "summer", "Summer", "desc", "img", true, 1,
                Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-06-01T00:00:00Z"));
    }
}
