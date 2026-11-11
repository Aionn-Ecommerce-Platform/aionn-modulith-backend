package com.aionn.catalog.application.port.out.merchant;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Curated product collections (campaign landing pages, seasonal edits). */
public interface CollectionPersistencePort {

    List<CollectionView> findAll(boolean activeOnly);

    Optional<CollectionView> findByIdOrSlug(String key);

    CollectionView save(CollectionView collection);

    void delete(String collectionId);

    long countProducts(String collectionId);

    boolean existsById(String collectionId);

    boolean existsAll(Collection<String> collectionIds);

    record CollectionView(
            String collectionId,
            String slug,
            String name,
            String description,
            String imageUrl,
            boolean active,
            int displayOrder,
            Instant createdAt,
            Instant updatedAt) {
    }
}
