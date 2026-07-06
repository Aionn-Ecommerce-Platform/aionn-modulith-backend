package com.aionn.catalog.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.catalog.domain.event.BrandEvents;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.valueobject.BrandStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class Brand extends AggregateRoot {

    private final String brandId;
    private String name;
    private String logoUrl;
    private String description;
    private BrandStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<Translation> translations = new ArrayList<>();

    public record Translation(String locale, String name, String description) {
    }

    public List<Translation> translations() {
        return Collections.unmodifiableList(translations);
    }

    public Brand(
            String brandId,
            String name,
            String logoUrl,
            String description,
            BrandStatus status,
            Instant createdAt,
            Instant updatedAt,
            List<Translation> translations) {
        this.brandId = brandId;
        this.name = name;
        this.logoUrl = logoUrl;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        if (translations != null) {
            this.translations.addAll(translations);
        }
    }

    public static Brand create(String brandId, String name, String logoUrl, String description) {
        Guard.require(name != null && !name.isBlank(),
                () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT, "name must not be blank"));
        Instant now = Instant.now();
        String trimmedName = name.trim();
        Brand brand = new Brand(brandId, trimmedName, logoUrl, description, BrandStatus.ACTIVE, now, now, List.of());
        brand.registerEvent(new BrandEvents.BrandCreated(brandId, trimmedName, logoUrl, description, now));
        return brand;
    }

    public void update(String name, String logoUrl, String description) {
        Guard.require(status != BrandStatus.DELETED,
                () -> new CatalogException(CatalogErrorCode.BRAND_NOT_FOUND, "Cannot update deleted brand"));
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (logoUrl != null) {
            this.logoUrl = logoUrl;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedAt = Instant.now();
        registerEvent(new BrandEvents.BrandUpdated(brandId, this.name, this.logoUrl, this.description, updatedAt));
    }

    public void softDelete(String reason) {
        if (status == BrandStatus.DELETED) {
            return;
        }
        this.status = BrandStatus.DELETED;
        this.updatedAt = Instant.now();
        registerEvent(new BrandEvents.BrandDeleted(brandId, reason, updatedAt, updatedAt));
    }

    @Override
    protected String aggregateId() {
        return brandId;
    }
}
