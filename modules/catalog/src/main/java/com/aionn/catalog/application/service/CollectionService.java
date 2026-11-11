package com.aionn.catalog.application.service;

import com.aionn.catalog.application.port.out.merchant.CollectionPersistencePort;
import com.aionn.catalog.application.port.out.merchant.CollectionPersistencePort.CollectionView;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class CollectionService {

    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]++(?:-[a-z0-9]++)*+$");

    private final CollectionPersistencePort collectionRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<CollectionView> list(boolean activeOnly) {
        return collectionRepository.findAll(activeOnly);
    }

    @Transactional(readOnly = true)
    public CollectionView get(String key) {
        return collectionRepository.findByIdOrSlug(key)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.COLLECTION_NOT_FOUND));
    }

    public CollectionView create(String slug, String name, String description, String imageUrl,
            boolean active, int displayOrder) {
        String normalizedSlug = requireSlug(slug);
        String normalizedName = requireName(name);
        Instant now = clock.instant();
        CollectionView view = new CollectionView(
                "col_" + IdGenerator.ulid(),
                normalizedSlug,
                normalizedName,
                blankToNull(description),
                blankToNull(imageUrl),
                active,
                Math.max(0, displayOrder),
                now,
                now);
        try {
            return collectionRepository.save(view);
        } catch (DuplicateKeyException ex) {
            throw new CatalogException(CatalogErrorCode.COLLECTION_SLUG_CONFLICT);
        }
    }

    public CollectionView update(String collectionId, String slug, String name, String description,
            String imageUrl, boolean active, int displayOrder) {
        CollectionView existing = get(collectionId);
        Instant now = clock.instant();
        CollectionView updated = new CollectionView(
                existing.collectionId(),
                slug == null ? existing.slug() : requireSlug(slug),
                name == null ? existing.name() : requireName(name),
                description == null ? existing.description() : blankToNull(description),
                imageUrl == null ? existing.imageUrl() : blankToNull(imageUrl),
                active,
                Math.max(0, displayOrder),
                existing.createdAt(),
                now);
        try {
            return collectionRepository.save(updated);
        } catch (DuplicateKeyException ex) {
            throw new CatalogException(CatalogErrorCode.COLLECTION_SLUG_CONFLICT);
        }
    }

    public void delete(String collectionId) {
        CollectionView existing = get(collectionId);
        if (collectionRepository.countProducts(existing.collectionId()) > 0) {
            throw new CatalogException(CatalogErrorCode.COLLECTION_HAS_PRODUCTS);
        }
        collectionRepository.delete(existing.collectionId());
    }

    @Transactional(readOnly = true)
    public void requireExisting(Collection<String> collectionIds) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            return;
        }
        if (!collectionRepository.existsAll(collectionIds)) {
            throw new CatalogException(CatalogErrorCode.COLLECTION_NOT_FOUND,
                    "One or more collection ids do not exist");
        }
    }

    private static String requireSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new CatalogException(CatalogErrorCode.COLLECTION_INVALID, "slug must not be blank");
        }
        String normalized = slug.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 100 || !SLUG.matcher(normalized).matches()) {
            throw new CatalogException(CatalogErrorCode.COLLECTION_INVALID,
                    "slug must contain lowercase letters, digits and single hyphens");
        }
        return normalized;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new CatalogException(CatalogErrorCode.COLLECTION_INVALID, "name must not be blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 255) {
            throw new CatalogException(CatalogErrorCode.COLLECTION_INVALID,
                    "name must be at most 255 characters");
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
